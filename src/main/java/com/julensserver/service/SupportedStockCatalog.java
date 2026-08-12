package com.julensserver.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class SupportedStockCatalog {

    private static final List<SupportedStock> STOCKS = List.of(
            stock("AAPL", "애플"),
            stock("MSFT", "마이크로소프트"),
            stock("NVDA", "엔비디아"),
            stock("AMZN", "아마존"),
            stock("GOOGL", "알파벳 A"),
            stock("GOOG", "알파벳 C"),
            stock("META", "메타 플랫폼스"),
            stock("TSLA", "테슬라"),
            stock("AVGO", "브로드컴"),
            stock("ORCL", "오라클"),
            stock("NFLX", "넷플릭스"),
            stock("COST", "코스트코"),
            stock("AMD", "AMD"),
            stock("ADBE", "어도비"),
            stock("CSCO", "시스코"),
            stock("QCOM", "퀄컴"),
            stock("TXN", "텍사스 인스트루먼트"),
            stock("INTU", "인튜이트"),
            stock("ISRG", "인튜이티브 서지컬"),
            stock("AMGN", "암젠"),
            stock("NOW", "서비스나우"),
            stock("AMAT", "어플라이드 머티어리얼즈"),
            stock("BKNG", "부킹 홀딩스"),
            stock("ADP", "ADP"),
            stock("GILD", "길리어드 사이언스"),
            stock("ADI", "아날로그 디바이시스"),
            stock("KLAC", "KLA"),
            stock("LRCX", "램 리서치"),
            stock("PANW", "팔로알토 네트웍스"),
            stock("MU", "마이크론"),
            stock("MDLZ", "몬델리즈"),
            stock("SBUX", "스타벅스"),
            stock("REGN", "리제네론"),
            stock("VRTX", "버텍스"),
            stock("SNPS", "시놉시스"),
            stock("CDNS", "케이던스 디자인 시스템즈"),
            stock("MAR", "메리어트 인터내셔널"),
            stock("ABNB", "에어비앤비"),
            stock("CRWD", "크라우드스트라이크"),
            stock("MRVL", "마벨 테크놀로지"),
            stock("FTNT", "포티넷"),
            stock("PYPL", "페이팔"),
            stock("CSX", "CSX"),
            stock("NXPI", "NXP 세미컨덕터"),
            stock("WDAY", "워크데이"),
            stock("CEG", "콘스텔레이션 에너지"),
            stock("FAST", "패스널"),
            stock("PCAR", "파카"),
            stock("ROST", "로스 스토어스"),
            stock("CTAS", "신타스"),
            stock("BRK.B", "버크셔 해서웨이 B"),
            stock("JPM", "JP모건 체이스"),
            stock("V", "비자"),
            stock("WMT", "월마트"),
            stock("XOM", "엑슨모빌"),
            stock("MA", "마스터카드"),
            stock("UNH", "유나이티드헬스 그룹"),
            stock("LLY", "일라이 릴리"),
            stock("HD", "홈디포"),
            stock("PG", "프록터 앤드 갬블"),
            stock("JNJ", "존슨앤드존슨"),
            stock("ABBV", "애브비"),
            stock("BAC", "뱅크오브아메리카"),
            stock("KO", "코카콜라"),
            stock("MRK", "머크"),
            stock("CVX", "셰브론"),
            stock("PEP", "펩시코"),
            stock("TMO", "써모 피셔 사이언티픽"),
            stock("WFC", "웰스파고"),
            stock("LIN", "린데"),
            stock("MCD", "맥도날드"),
            stock("ACN", "액센츄어"),
            stock("ABT", "애보트 래버러토리스"),
            stock("DHR", "다나허"),
            stock("GE", "GE 에어로스페이스"),
            stock("CAT", "캐터필러"),
            stock("PM", "필립 모리스"),
            stock("IBM", "IBM"),
            stock("VZ", "버라이즌"),
            stock("DIS", "월트 디즈니"),
            stock("NEE", "넥스트에라 에너지"),
            stock("RTX", "RTX"),
            stock("GS", "골드만삭스"),
            stock("SPGI", "S&P 글로벌"),
            stock("LOW", "로우스"),
            stock("PFE", "화이자"),
            stock("T", "AT&T"),
            stock("HON", "허니웰"),
            stock("COP", "코노코필립스"),
            stock("BLK", "블랙록"),
            stock("SYK", "스트라이커"),
            stock("TJX", "TJX 컴퍼니"),
            stock("PLD", "프로로지스"),
            stock("ETN", "이튼"),
            stock("BSX", "보스턴 사이언티픽"),
            stock("UPS", "UPS"),
            stock("C", "씨티그룹"),
            stock("SCHW", "찰스 슈왑"),
            stock("DE", "디어앤컴퍼니"),
            stock("MMC", "마시 맥레넌")
    );

    public List<SupportedStock> stocks() {
        return STOCKS;
    }

    private static SupportedStock stock(
            String ticker,
            String companyNameKr
    ) {
        return new SupportedStock(ticker, companyNameKr);
    }

    public record SupportedStock(
            String ticker,
            String companyNameKr
    ) {
        public SupportedStock {
            if (ticker == null || ticker.isBlank()
                    || companyNameKr == null
                    || companyNameKr.isBlank()) {
                throw new IllegalArgumentException(
                        "지원 종목의 티커와 한국어명은 필수입니다."
                );
            }
            ticker = ticker.trim().toUpperCase(Locale.ROOT);
            companyNameKr = companyNameKr.trim();
        }
    }
}
