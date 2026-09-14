package com.eatda.app.data.api

import com.eatda.app.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object RecipeApiService {

    // 식품안전나라에서 발급받은 인증키
    private const val API_KEY = "키 입력"

    suspend fun fetchRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        try {
            // 식품안전나라 레시피 1~100번 조회
            val url =
                "https://openapi.foodsafetykorea.go.kr/api/$API_KEY/COOKRCP01/json/1/100"

            val jsonText = URL(url).readText()
            val json = JSONObject(jsonText)

            val cookObject = json.optJSONObject("COOKRCP01")
                ?: return@withContext emptyList()

            val rows = cookObject.optJSONArray("row")
                ?: return@withContext emptyList()

            val recipes = mutableListOf<Recipe>()

            for (i in 0 until rows.length()) {

                val item = rows.getJSONObject(i)

                // ─────────────────────────────
                // 1. 레시피 기본 정보
                // ─────────────────────────────

                // 레시피 이름
                val title = item.optString("RCP_NM")

                // 레시피 음식 사진
                val imageUrl = item.optString("ATT_FILE_NO_MAIN")

                // 재료 목록
                val ingredientsText = item.optString("RCP_PARTS_DTLS")

                // 레시피 설명 / 요리 종류
                val description = item.optString("RCP_PAT2")


                // ─────────────────────────────
                // 2. 영양 정보
                // ─────────────────────────────

                // 열량 (kcal)
                val calories =
                    item.optString("INFO_ENG")
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0

                // 탄수화물 (g)
                val carbohydrates =
                    item.optString("INFO_CAR")
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0

                // 지방 (g)
                val fat =
                    item.optString("INFO_FAT")
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0

                // 단백질 (g)
                val protein =
                    item.optString("INFO_PRO")
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0

                // 나트륨 (mg)
                val sodium =
                    item.optString("INFO_NA")
                        .replace(",", "")
                        .toDoubleOrNull() ?: 0.0


                // ─────────────────────────────
                // 3. 재료 문자열 → 리스트
                // ─────────────────────────────

                val ingredients = ingredientsText
                    .replace("•", ",")
                    .replace("·", ",")
                    .replace("\n", ",")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }


                // ─────────────────────────────
                // 4. 조리 과정
                // MANUAL01 ~ MANUAL20
                // ─────────────────────────────

                val cookingSteps = mutableListOf<String>()

                for (stepNumber in 1..20) {

                    val key =
                        "MANUAL" + String.format("%02d", stepNumber)

                    val step =
                        item.optString(key).trim()

                    if (step.isNotBlank()) {
                        cookingSteps.add(step)
                    }
                }


                // ─────────────────────────────
                // 첫 번째 레시피 확인용 로그
                // ─────────────────────────────

                if (i == 0) {

                    println("===== 첫 번째 레시피 =====")
                    println("제목: $title")
                    println("이미지 URL: $imageUrl")
                    println("원본 재료: $ingredientsText")
                    println("파싱된 재료: $ingredients")

                    println("조리 과정 개수: ${cookingSteps.size}")
                    println("조리 과정: $cookingSteps")

                    println("열량: $calories kcal")
                    println("탄수화물: $carbohydrates g")
                    println("지방: $fat g")
                    println("단백질: $protein g")
                    println("나트륨: $sodium mg")
                }


                // ─────────────────────────────
                // 5. 추천 태그
                // ─────────────────────────────

                val tag =
                    when {
                        protein >= 15 -> "단백질"
                        else -> "일반"
                    }


                // ─────────────────────────────
                // 6. 조리 시간
                // ─────────────────────────────

                // 식품안전나라 API에서 현재 별도 조리시간 값을 사용하지 않음
                val minutes = 20


                // ─────────────────────────────
                // 7. Recipe 객체 생성
                // ─────────────────────────────

                recipes.add(
                    Recipe(
                        id = "api_$i",

                        title = title,

                        tag = tag,

                        minutes = minutes,

                        ingredients = ingredients,

                        priority = null,

                        description = description,

                        // 조리 과정
                        cookingSteps = cookingSteps,

                        // 영양 정보
                        calories = calories,
                        carbohydrates = carbohydrates,
                        protein = protein,
                        fat = fat,
                        sodium = sodium,

                        // 레시피 음식 사진
                        imageUrl = imageUrl,
                    )
                )
            }

            recipes

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }
}