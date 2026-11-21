package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.MedicineCategory
import com.example.data.model.Product
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CatalogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedIn(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in.")
    }

    fun observeCategories(): Flow<List<MedicineCategory>> =
        db.collection("categories")
            .snapshots()
            .map { snap ->
                snap.toObjects(MedicineCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }

    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .snapshots()
            .map { snap ->
                snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    suspend fun saveCategory(category: MedicineCategory): Result<Unit> {
        val catId = category.id.ifBlank { db.collection("categories").document().id }
        val path = "categories/$catId"
        return try {
            requireSignedIn()
            require(category.nameEn.trim().length in 2..60) { "English name must be 2-60 chars." }
            require(category.nameUr.trim().length in 2..80) { "Urdu name must be 2-80 chars." }
            val ref = db.collection("categories").document(catId)
            if (category.id.isBlank()) {
                ref.set(
                    mapOf(
                        "nameEn" to category.nameEn.trim(),
                        "nameUr" to category.nameUr.trim(),
                        "iconKey" to category.iconKey.ifBlank { "medication" },
                        "sortOrder" to category.sortOrder.coerceIn(0L, 999L),
                        "active" to category.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "nameEn" to category.nameEn.trim(),
                        "nameUr" to category.nameUr.trim(),
                        "iconKey" to category.iconKey.ifBlank { "medication" },
                        "sortOrder" to category.sortOrder.coerceIn(0L, 999L),
                        "active" to category.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val path = "categories/$categoryId"
        return try {
            requireSignedIn()
            require(categoryId.isNotBlank())
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        val prodId = product.id.ifBlank { db.collection("products").document().id }
        val path = "products/$prodId"
        return try {
            requireSignedIn()
            require(product.nameEn.trim().length in 2..120) { "English name must be 2-120 chars." }
            require(product.nameUr.trim().length in 2..140) { "Urdu name must be 2-140 chars." }
            require(product.genericName.trim().length in 2..120) { "Generic name required." }
            require(product.manufacturer.trim().length in 2..80) { "Manufacturer required." }
            require(product.categoryId.isNotBlank()) { "Category required." }
            require(product.pricePkr > 0.0 && product.pricePkr <= 1_000_000.0) { "Invalid price." }
            require(product.discountPercent in 0L..90L) { "Discount must be 0-90%." }
            require(product.stock in 0L..100_000L) { "Invalid stock." }

            val baseFields = mutableMapOf<String, Any?>(
                "nameEn" to product.nameEn.trim(),
                "nameUr" to product.nameUr.trim(),
                "genericName" to product.genericName.trim(),
                "manufacturer" to product.manufacturer.trim(),
                "categoryId" to product.categoryId,
                "descriptionEn" to product.descriptionEn.trim().ifBlank { "Verified pharmacy formulation." },
                "descriptionUr" to product.descriptionUr.trim().ifBlank { "مستند فارمیسی دوا۔" },
                "packSize" to product.packSize.trim().ifBlank { "1 Pack" },
                "pricePkr" to product.pricePkr,
                "discountPercent" to product.discountPercent,
                "stock" to product.stock,
                "prescriptionRequired" to product.prescriptionRequired,
                "popular" to product.popular,
                "active" to product.active,
                "imageUrl" to product.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                "imageBase64" to product.imageBase64?.takeIf { it.isNotEmpty() },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val ref = db.collection("products").document(prodId)
            if (product.id.isBlank()) {
                baseFields["createdAt"] = FieldValue.serverTimestamp()
                @Suppress("UNCHECKED_CAST")
                val payload = baseFields.filterValues { it != null } as Map<String, Any>
                ref.set(payload).await()
            } else {
                @Suppress("UNCHECKED_CAST")
                val payload = baseFields.filterValues { it != null } as Map<String, Any>
                ref.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireSignedIn()
            require(productId.isNotBlank())
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Invoked only when an authenticated verified Admin signs in and the catalog is empty.
     * Ensures real Firestore documents exist on first launch without bypassing security rules.
     */
    suspend fun seedStarterCatalogIfEmpty(): Result<Unit> {
        return try {
            requireSignedIn()
            val existingCats = db.collection("categories").limit(1).get().await()
            val existingProds = db.collection("products").limit(1).get().await()
            if (!existingCats.isEmpty && !existingProds.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            if (existingCats.isEmpty) {
                starterCategories().forEach { (id, payload) ->
                    batch.set(db.collection("categories").document(id), payload)
                }
            }
            if (existingProds.isEmpty) {
                starterProducts().forEach { (id, payload) ->
                    batch.set(db.collection("products").document(id), payload)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "catalog/seed")
            Result.failure(e)
        }
    }

    private fun starterCategories(): List<Pair<String, Map<String, Any>>> = listOf(
        "cat_pain" to mapOf(
            "nameEn" to "Pain & Fever",
            "nameUr" to "درد اور بخار",
            "iconKey" to "healing",
            "sortOrder" to 1L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_antibiotics" to mapOf(
            "nameEn" to "Antibiotics",
            "nameUr" to "اینٹی بایوٹکس",
            "iconKey" to "medication",
            "sortOrder" to 2L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_vitamins" to mapOf(
            "nameEn" to "Vitamins",
            "nameUr" to "وٹامنز",
            "iconKey" to "wellness",
            "sortOrder" to 3L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_cardiac" to mapOf(
            "nameEn" to "Heart & BP",
            "nameUr" to "دل اور بلڈ پریشر",
            "iconKey" to "heart",
            "sortOrder" to 4L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_diabetes" to mapOf(
            "nameEn" to "Diabetes Care",
            "nameUr" to "ذیابیطس کیئر",
            "iconKey" to "monitor",
            "sortOrder" to 5L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_derma" to mapOf(
            "nameEn" to "Skin & Derma",
            "nameUr" to "جلد کی حفاظت",
            "iconKey" to "derma",
            "sortOrder" to 6L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_baby" to mapOf(
            "nameEn" to "Baby Care",
            "nameUr" to "بچوں کی دیکھ بھال",
            "iconKey" to "baby",
            "sortOrder" to 7L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "cat_firstaid" to mapOf(
            "nameEn" to "First Aid",
            "nameUr" to "ابتدائی طبی امداد",
            "iconKey" to "firstaid",
            "sortOrder" to 8L,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    private fun starterProducts(): List<Pair<String, Map<String, Any>>> = listOf(
        "prod_panadol_extra" to mapOf(
            "nameEn" to "Panadol Extra Tablets",
            "nameUr" to "پیناڈول ایکسٹرا گولیاں",
            "genericName" to "Paracetamol 500mg + Caffeine 65mg",
            "manufacturer" to "Haleon / GSK Pakistan",
            "categoryId" to "cat_pain",
            "descriptionEn" to "Over-the-counter analgesic formulation in sealed manufacturer blister strips. Consult label warnings before use.",
            "descriptionUr" to "درد اور بخار کے لیے او ٹی سی گولیاں۔ استعمال سے قبل پیکج پر درج ہدایات ملاحظہ کریں۔",
            "packSize" to "20 Tablets (2 Strips)",
            "pricePkr" to 160.0,
            "discountPercent" to 10L,
            "stock" to 85L,
            "prescriptionRequired" to false,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_augmentin_625" to mapOf(
            "nameEn" to "Augmentin 625mg Tablets",
            "nameUr" to "آگمینٹن 625mg گولیاں",
            "genericName" to "Co-Amoxiclav 625mg",
            "manufacturer" to "GSK Pakistan",
            "categoryId" to "cat_antibiotics",
            "descriptionEn" to "Prescription-only antibacterial pack. Requires valid physician prescription verified by a DocMeds licensed pharmacist.",
            "descriptionUr" to "نسخے کے ساتھ دستیاب اینٹی بایوٹک۔ آرڈر کی منظوری کے لیے مستند ڈاکٹر کا نسخہ لازمی ہے۔",
            "packSize" to "6 Film-Coated Tablets",
            "pricePkr" to 540.0,
            "discountPercent" to 5L,
            "stock" to 42L,
            "prescriptionRequired" to true,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_cac_1000" to mapOf(
            "nameEn" to "CaC-1000 Plus Orange",
            "nameUr" to "سی اے سی 1000 پلس اورنج",
            "genericName" to "Calcium + Vitamin C + Vitamin D3",
            "manufacturer" to "Haleon Pakistan",
            "categoryId" to "cat_vitamins",
            "descriptionEn" to "Effervescent calcium and vitamin D3 supplement tube in sealed pharmacy packaging.",
            "descriptionUr" to "کیلشیم اور وٹامن ڈی 3 سپلیمنٹ ٹیوب۔",
            "packSize" to "10 Effervescent Tablets",
            "pricePkr" to 380.0,
            "discountPercent" to 15L,
            "stock" to 60L,
            "prescriptionRequired" to false,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_concor_5" to mapOf(
            "nameEn" to "Concor 5mg Tablets",
            "nameUr" to "کونکور 5mg گولیاں",
            "genericName" to "Bisoprolol Fumarate 5mg",
            "manufacturer" to "Merck Pakistan",
            "categoryId" to "cat_cardiac",
            "descriptionEn" to "Cardiovascular prescription medicine. Pharmacist verification of a current physician prescription is mandatory.",
            "descriptionUr" to "دل اور بلڈ پریشر کی نسخہ والی دوا۔ فارماسسٹ کی تصدیق لازمی ہے۔",
            "packSize" to "14 Tablets",
            "pricePkr" to 490.0,
            "discountPercent" to 0L,
            "stock" to 30L,
            "prescriptionRequired" to true,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_glucophage_500" to mapOf(
            "nameEn" to "Glucophage 500mg",
            "nameUr" to "گلوکوفیج 500mg",
            "genericName" to "Metformin Hydrochloride 500mg",
            "manufacturer" to "Martin Dow",
            "categoryId" to "cat_diabetes",
            "descriptionEn" to "Prescription glycemic management tablets dispensed only after authorized pharmacist review.",
            "descriptionUr" to "ذیابیطس کی نسخہ والی دوا جو فارماسسٹ کے جائزے کے بعد فراہم کی جاتی ہے۔",
            "packSize" to "50 Tablets",
            "pricePkr" to 310.0,
            "discountPercent" to 8L,
            "stock" to 55L,
            "prescriptionRequired" to true,
            "popular" to false,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_surbex_z" to mapOf(
            "nameEn" to "Surbex Z Multivitamin",
            "nameUr" to "سربیکس زی ملٹی وٹامن",
            "genericName" to "Vitamin B-Complex + Vitamin C + Zinc",
            "manufacturer" to "Abbott Laboratories",
            "categoryId" to "cat_vitamins",
            "descriptionEn" to "Daily multivitamin and mineral supplement bottle in original tamper-evident seal.",
            "descriptionUr" to "روزمرہ ملٹی وٹامن اور زنک سپلیمنٹ۔",
            "packSize" to "30 Film-Coated Tablets",
            "pricePkr" to 620.0,
            "discountPercent" to 12L,
            "stock" to 48L,
            "prescriptionRequired" to false,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_brufen_400" to mapOf(
            "nameEn" to "Brufen 400mg Tablets",
            "nameUr" to "بروفن 400mg گولیاں",
            "genericName" to "Ibuprofen 400mg",
            "manufacturer" to "Abbott Laboratories",
            "categoryId" to "cat_pain",
            "descriptionEn" to "Anti-inflammatory pain relief pack. Review manufacturer leaflet and consult pharmacist if taking other medications.",
            "descriptionUr" to "سوزش اور درد کے لیے گولیاں۔ دیگر ادویات کے ساتھ استعمال سے قبل فارماسسٹ سے مشورہ کریں۔",
            "packSize" to "20 Tablets",
            "pricePkr" to 145.0,
            "discountPercent" to 0L,
            "stock" to 70L,
            "prescriptionRequired" to false,
            "popular" to false,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_cetaphil_cleanser" to mapOf(
            "nameEn" to "Cetaphil Gentle Cleanser",
            "nameUr" to "سیٹافل جینٹل کلینزر",
            "genericName" to "Non-Comedogenic Skin Cleanser",
            "manufacturer" to "Galderma",
            "categoryId" to "cat_derma",
            "descriptionEn" to "Dermatologist-tested gentle skin cleanser suitable for sensitive skin.",
            "descriptionUr" to "حساس جلد کے لیے نرم اور محفوظ کلینزر۔",
            "packSize" to "236 ml Bottle",
            "pricePkr" to 2450.0,
            "discountPercent" to 10L,
            "stock" to 18L,
            "prescriptionRequired" to false,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_sudocrem" to mapOf(
            "nameEn" to "Sudocrem Antiseptic Cream",
            "nameUr" to "سوڈوکریم اینٹی سیپٹک کریم",
            "genericName" to "Zinc Oxide Protective Cream",
            "manufacturer" to "Teva",
            "categoryId" to "cat_baby",
            "descriptionEn" to "Soothing protective barrier cream for baby skin care and minor skin irritation.",
            "descriptionUr" to "بچوں کی جلد کی حفاظت کے لیے آرام دہ کریم۔",
            "packSize" to "125g Tub",
            "pricePkr" to 1350.0,
            "discountPercent" to 7L,
            "stock" to 22L,
            "prescriptionRequired" to false,
            "popular" to false,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_dettol_firstaid" to mapOf(
            "nameEn" to "Sterile First Aid Dressings",
            "nameUr" to "اسٹیرائل فرسٹ ایڈ پٹیاں",
            "genericName" to "Sterile Gauze & Antiseptic Kit",
            "manufacturer" to "MediCare Supplies",
            "categoryId" to "cat_firstaid",
            "descriptionEn" to "Compact home and travel first-aid kit with sterile bandages, swabs, and medical tape.",
            "descriptionUr" to "گھر اور سفر کے لیے مکمل ابتدائی طبی امداد کٹ۔",
            "packSize" to "24-Piece Kit",
            "pricePkr" to 890.0,
            "discountPercent" to 15L,
            "stock" to 34L,
            "prescriptionRequired" to false,
            "popular" to false,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_azomax_500" to mapOf(
            "nameEn" to "Azomax 500mg Capsules",
            "nameUr" to "ایزومیکس 500mg کیپسول",
            "genericName" to "Azithromycin 500mg",
            "manufacturer" to "Sandoz",
            "categoryId" to "cat_antibiotics",
            "descriptionEn" to "Prescription macrolide antibiotic pack. Valid physician Rx required for pharmacist clearance.",
            "descriptionUr" to "نسخے کے ساتھ دستیاب اینٹی بایوٹک کیپسول۔",
            "packSize" to "3 Capsules",
            "pricePkr" to 420.0,
            "discountPercent" to 0L,
            "stock" to 0L, // Demonstrates real out-of-stock state in catalog!
            "prescriptionRequired" to true,
            "popular" to false,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ),
        "prod_accu_strips" to mapOf(
            "nameEn" to "Accu-Chek Active Strips",
            "nameUr" to "ایکو چیک ایکٹو اسٹرپس",
            "genericName" to "Blood Glucose Test Strips",
            "manufacturer" to "Roche Diagnostics",
            "categoryId" to "cat_diabetes",
            "descriptionEn" to "Diagnostic glucose monitoring strips sealed for Accu-Chek Active meters.",
            "descriptionUr" to "شوگر چیک کرنے کی مستند ٹیسٹ اسٹرپس۔",
            "packSize" to "50 Test Strips",
            "pricePkr" to 2850.0,
            "discountPercent" to 5L,
            "stock" to 4L, // Low stock alert demo for Admin Dashboard!
            "prescriptionRequired" to false,
            "popular" to true,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )
}
