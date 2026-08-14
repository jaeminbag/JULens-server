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

    @Test
    void 주식_클래스_문자는_한글로_음역하지_않는다() {
        assertEquals(
                "클라우드 홀딩스 A",
                service.resolve(
                        "TEST",
                        "Cloud Holdings Cl A",
                        null
                )
        );
        assertEquals(
                "클라우드 홀딩스 B",
                service.resolve(
                        "TEST",
                        "Cloud Holdings Class B",
                        null
                )
        );
    }

    @Test
    void 잘못_저장된_자동_음역명은_검증된_종목명으로_복구한다() {
        assertEquals(
                "파운더 그룹 A",
                service.resolve(
                        "FGL",
                        "FOUNDER GROUP LTD-CLASS A",
                        "파운더 그룹 애"
                )
        );
        assertEquals(
                "더마타 테라퓨틱스",
                service.resolve(
                        "DRMA",
                        "DERMATA THERAPEUTICS INC",
                        "더매태 테라퓨틱스"
                )
        );
        assertEquals(
                "하오신 홀딩스 A",
                service.resolve(
                        "HXHX",
                        "HAOXIN HOLDINGS LTD-CL A",
                        "하오크신 홀딩스 CL 애"
                )
        );
    }

    @Test
    void 거래량_화면에서_확인된_회사명을_자연스럽게_표기한다() {
        assertEquals(
                "하이퍼스케일 데이터",
                service.resolve(
                        "GPUS",
                        "HYPERSCALE DATA INC",
                        "히퍼스캐레 DATA"
                )
        );
        assertEquals(
                "리콘 테크놀로지 A",
                service.resolve(
                        "RCON",
                        "RECON TECHNOLOGY LTD-CLASS A",
                        "RECON 테크놀로지 CLASS 애"
                )
        );
        assertEquals(
                "리미나투스 파마 A",
                service.resolve(
                        "LIMN",
                        "LIMINATUS PHARMA INC-CL A",
                        "리미내터스 파마 CL 애"
                )
        );
        assertEquals(
                "망고슈티컬스",
                service.resolve(
                        "MGRX",
                        "MANGOCEUTICALS INC",
                        "매노케어티캘스"
                )
        );
        assertEquals(
                "온다스",
                service.resolve("ONDS", "ONDAS INC", "ONDAS INC")
        );
    }

    @Test
    void 이전_음역기의_영문_토큰과_클래스_애를_다시_변환한다() {
        assertEquals(
                "하이퍼스케일 데이터 A",
                service.resolve(
                        "TEST",
                        "HYPERSCALE DATA INC-CLASS A",
                        "히퍼스캐레 DATA CLASS 애"
                )
        );
    }
}
