package com.julensserver.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KoreanCompanyNameServiceTest {

    private final KoreanCompanyNameService service =
            new KoreanCompanyNameService();

    @Test
    void 기존_한글명이_있으면_자동_음역으로_덮어쓰지_않는다() {
        assertEquals(
                "마이크론 테크놀로지",
                service.resolve(
                        "MU",
                        "MICRON TECHNOLOGY INC",
                        "마이크론 테크놀로지"
                )
        );
    }

    @Test
    void 영문이_복사된_신규종목은_한글_음역명으로_변환한다() {
        assertEquals(
                "차우차우 클라우드 인터내셔널 홀딩스",
                service.resolve(
                        "CHOW",
                        "ChowChow Cloud International Holdings Limited",
                        "ChowChow Cloud International Holdings Limited"
                )
        );
        assertEquals(
                "로키 마운틴 초콜릿 팩토리",
                service.resolve(
                        "RMCF",
                        "Rocky Mountain Chocolate Factory Inc",
                        null
                )
        );
        assertEquals(
                "OFA 그룹",
                service.resolve("OFA", "OFA Group Inc", "OFA Group Inc")
        );
    }

    @Test
    void 알려지지_않은_단어도_한글_발음으로_대체한다() {
        assertEquals(
                "비보스 테크놀로지스",
                service.resolve(
                        "VVOS",
                        "Vivos Technologies Inc",
                        "Vivos Technologies Inc"
                )
        );
    }
}
