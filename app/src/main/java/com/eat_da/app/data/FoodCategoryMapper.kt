package com.eatda.app.data

import com.eatda.app.data.model.FoodCategory

object FoodCategoryMapper {

    fun getCategory(foodName: String): FoodCategory {
        val name = foodName
            .trim()
            .replace(" ", "")
            .lowercase()

        return when {
            // 육류
            containsAny(
                name,
                "삼겹살", "돼지고기", "목살", "앞다리살", "뒷다리살",
                "소고기", "쇠고기", "등심", "안심", "갈비",
                "닭고기", "닭가슴살", "닭다리", "오리고기",
                "양고기"
            ) -> FoodCategory.MEAT

            // 해산물
            containsAny(
                name,
                "연어", "고등어", "갈치", "참치", "꽁치",
                "오징어", "낙지", "문어", "주꾸미",
                "새우", "게", "꽃게",
                "조개", "바지락", "홍합", "전복",
                "굴", "멸치"
            ) -> FoodCategory.SEAFOOD

            // 유제품
            containsAny(
                name,
                "우유", "치즈", "요거트", "요구르트",
                "버터", "생크림", "휘핑크림"
            ) -> FoodCategory.DAIRY

            // 과일
            containsAny(
                name,
                "사과", "배", "바나나", "딸기", "포도",
                "귤", "오렌지", "레몬", "라임",
                "복숭아", "자두", "수박", "참외",
                "키위", "망고", "파인애플", "블루베리",
                "체리", "감", "석류", "아보카도"
            ) -> FoodCategory.FRUIT

            // 채소
            containsAny(
                name,
                "양배추", "배추", "상추", "깻잎",
                "시금치", "브로콜리", "당근", "무",
                "오이", "애호박", "호박", "가지",
                "양파", "대파", "쪽파", "마늘",
                "고추", "파프리카", "피망",
                "감자", "고구마",
                "버섯", "콩나물", "숙주",
                "토마토"
            ) -> FoodCategory.VEGETABLE

            // 음료
            containsAny(
                name,
                "주스", "쥬스", "탄산음료",
                "콜라", "사이다", "커피",
                "생수", "물", "차"
            ) -> FoodCategory.BEVERAGE

            // 소스 / 조미료
            containsAny(
                name,
                "간장", "고추장", "된장", "쌈장",
                "케첩", "마요네즈", "머스타드",
                "소스", "드레싱", "식초",
                "참기름", "들기름"
            ) -> FoodCategory.SAUCE

            // 가공식품
            containsAny(
                name,
                "햄", "소시지", "베이컨", "스팸",
                "어묵", "맛살", "두부",
                "라면", "만두",
                "김치", "피클"
            ) -> FoodCategory.PROCESSED

            // 곡물
            containsAny(
                name,
                "쌀", "현미", "보리", "귀리",
                "밀가루", "찹쌀", "잡곡",
                "콩", "옥수수"
            ) -> FoodCategory.GRAIN

            // 현재 FoodCategory에 기타(OTHER)가 없으므로
            // 기존 동작과 동일하게 미분류 식품은 GRAIN 처리
            else -> FoodCategory.GRAIN
        }
    }

    private fun containsAny(
        foodName: String,
        vararg keywords: String
    ): Boolean {
        return keywords.any { keyword ->
            foodName.contains(keyword)
        }
    }
}