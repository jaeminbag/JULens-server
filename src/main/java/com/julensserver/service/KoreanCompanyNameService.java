package com.julensserver.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KoreanCompanyNameService {

    private static final Map<String, String> TICKER_OVERRIDES = Map.of(
            "CHOW", "차우차우 클라우드 인터내셔널 홀딩스",
            "RMCF", "로키 마운틴 초콜릿 팩토리",
            "OFA", "OFA 그룹"
    );

    private static final Set<String> OMITTED_LEGAL_SUFFIXES = Set.of(
            "INC", "INCORPORATED", "LTD", "LIMITED", "PLC", "LLC",
            "LP", "L.P", "SA", "NV", "AG", "SE", "ADS", "ADR"
    );

    private static final Map<String, String> WORDS = Map.ofEntries(
            Map.entry("APPLE", "애플"),
            Map.entry("MICROSOFT", "마이크로소프트"),
            Map.entry("NVIDIA", "엔비디아"),
            Map.entry("MICRON", "마이크론"),
            Map.entry("AMAZON", "아마존"),
            Map.entry("ALPHABET", "알파벳"),
            Map.entry("META", "메타"),
            Map.entry("TESLA", "테슬라"),
            Map.entry("CLOUD", "클라우드"),
            Map.entry("INTERNATIONAL", "인터내셔널"),
            Map.entry("HOLDING", "홀딩"),
            Map.entry("HOLDINGS", "홀딩스"),
            Map.entry("GROUP", "그룹"),
            Map.entry("ROCKY", "로키"),
            Map.entry("MOUNTAIN", "마운틴"),
            Map.entry("CHOCOLATE", "초콜릿"),
            Map.entry("FACTORY", "팩토리"),
            Map.entry("TECH", "테크"),
            Map.entry("TECHNOLOGY", "테크놀로지"),
            Map.entry("TECHNOLOGIES", "테크놀로지스"),
            Map.entry("SYSTEM", "시스템"),
            Map.entry("SYSTEMS", "시스템스"),
            Map.entry("SOFTWARE", "소프트웨어"),
            Map.entry("SEMICONDUCTOR", "세미컨덕터"),
            Map.entry("SEMICONDUCTORS", "세미컨덕터스"),
            Map.entry("DIGITAL", "디지털"),
            Map.entry("NETWORK", "네트워크"),
            Map.entry("NETWORKS", "네트워크스"),
            Map.entry("COMMUNICATION", "커뮤니케이션"),
            Map.entry("COMMUNICATIONS", "커뮤니케이션스"),
            Map.entry("MEDIA", "미디어"),
            Map.entry("ENTERTAINMENT", "엔터테인먼트"),
            Map.entry("GLOBAL", "글로벌"),
            Map.entry("AMERICAN", "아메리칸"),
            Map.entry("UNITED", "유나이티드"),
            Map.entry("NATIONAL", "내셔널"),
            Map.entry("FIRST", "퍼스트"),
            Map.entry("NEW", "뉴"),
            Map.entry("ENERGY", "에너지"),
            Map.entry("POWER", "파워"),
            Map.entry("SOLAR", "솔라"),
            Map.entry("GREEN", "그린"),
            Map.entry("RESOURCES", "리소시스"),
            Map.entry("RESOURCE", "리소스"),
            Map.entry("INDUSTRIES", "인더스트리스"),
            Map.entry("INDUSTRIAL", "인더스트리얼"),
            Map.entry("CAPITAL", "캐피털"),
            Map.entry("FINANCIAL", "파이낸셜"),
            Map.entry("FINANCE", "파이낸스"),
            Map.entry("BANK", "뱅크"),
            Map.entry("TRUST", "트러스트"),
            Map.entry("INVESTMENT", "인베스트먼트"),
            Map.entry("INVESTMENTS", "인베스트먼츠"),
            Map.entry("ACQUISITION", "애퀴지션"),
            Map.entry("ACQUISITIONS", "애퀴지션스"),
            Map.entry("HEALTH", "헬스"),
            Map.entry("HEALTHCARE", "헬스케어"),
            Map.entry("MEDICAL", "메디컬"),
            Map.entry("MEDICINE", "메디슨"),
            Map.entry("PHARMA", "파마"),
            Map.entry("PHARMACEUTICAL", "파마슈티컬"),
            Map.entry("PHARMACEUTICALS", "파마슈티컬스"),
            Map.entry("THERAPEUTICS", "테라퓨틱스"),
            Map.entry("BIOTECH", "바이오테크"),
            Map.entry("BIOTECHNOLOGY", "바이오테크놀로지"),
            Map.entry("BIO", "바이오"),
            Map.entry("LIFE", "라이프"),
            Map.entry("SCIENCE", "사이언스"),
            Map.entry("SCIENCES", "사이언시스"),
            Map.entry("LAB", "랩"),
            Map.entry("LABS", "랩스"),
            Map.entry("LABORATORIES", "래버러토리스"),
            Map.entry("ROBOTICS", "로보틱스"),
            Map.entry("MOTORS", "모터스"),
            Map.entry("MOTOR", "모터"),
            Map.entry("AUTOMOTIVE", "오토모티브"),
            Map.entry("AIR", "에어"),
            Map.entry("AEROSPACE", "에어로스페이스"),
            Map.entry("SPACE", "스페이스"),
            Map.entry("MARINE", "마린"),
            Map.entry("SHIPPING", "쉬핑"),
            Map.entry("LOGISTICS", "로지스틱스"),
            Map.entry("FOODS", "푸즈"),
            Map.entry("FOOD", "푸드"),
            Map.entry("BEVERAGE", "베버리지"),
            Map.entry("RETAIL", "리테일"),
            Map.entry("CONSUMER", "컨슈머"),
            Map.entry("SERVICES", "서비스"),
            Map.entry("SERVICE", "서비스"),
            Map.entry("SOLUTIONS", "솔루션스"),
            Map.entry("SOLUTION", "솔루션"),
            Map.entry("VENTURES", "벤처스"),
            Map.entry("VENTURE", "벤처"),
            Map.entry("CORPORATION", "코퍼레이션"),
            Map.entry("CORP", "코퍼레이션"),
            Map.entry("COMPANY", "컴퍼니"),
            Map.entry("CO", "컴퍼니")
    );

    public String resolve(
            String ticker,
            String englishName,
            String existingKoreanName
    ) {
        if (containsHangul(existingKoreanName)) {
            return existingKoreanName.trim();
        }

        String normalizedTicker = ticker.trim().toUpperCase(Locale.ROOT);
        String override = TICKER_OVERRIDES.get(normalizedTicker);
        if (override != null) {
            return override;
        }

        List<String> localizedWords = new ArrayList<>();
        for (String token : splitWords(englishName)) {
            String normalized = token.toUpperCase(Locale.ROOT);
            if (OMITTED_LEGAL_SUFFIXES.contains(normalized)) {
                continue;
            }
            String knownWord = WORDS.get(normalized);
            if (knownWord != null) {
                localizedWords.add(knownWord);
                continue;
            }
            if (isAcronym(token)) {
                localizedWords.add(normalized);
                continue;
            }
            localizedWords.add(approximatePronunciation(token));
        }

        String localized = String.join(" ", localizedWords).trim();
        return localized.isEmpty() ? englishName.trim() : localized;
    }

    private List<String> splitWords(String name) {
        String separatedCamelCase = name.replaceAll(
                "(?<=[a-z])(?=[A-Z])",
                " "
        );
        String cleaned = separatedCamelCase.replaceAll(
                "[^A-Za-z0-9]+",
                " "
        ).trim();
        return cleaned.isEmpty() ? List.of() : List.of(cleaned.split("\\s+"));
    }

    private boolean isAcronym(String token) {
        return token.length() >= 2
                && token.length() <= 6
                && token.chars().allMatch(character ->
                Character.isUpperCase(character)
                        || Character.isDigit(character));
    }

    private boolean containsHangul(String value) {
        return value != null && value.codePoints().anyMatch(codePoint ->
                codePoint >= 0xAC00 && codePoint <= 0xD7A3);
    }

    private String approximatePronunciation(String word) {
        List<String> sounds = tokenizeSounds(word.toLowerCase(Locale.ROOT));
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < sounds.size(); index++) {
            String sound = sounds.get(index);
            String fixedPronunciation = fixedPronunciation(sound);
            if (fixedPronunciation != null) {
                result.append(fixedPronunciation);
                continue;
            }
            if (isVowel(sound)) {
                result.append(vowelWithoutOnset(sound));
                continue;
            }

            if (index + 1 < sounds.size() && isVowel(sounds.get(index + 1))) {
                result.append(syllable(sound, sounds.get(index + 1)));
                index++;
            } else if (isAttachableFinal(sound)
                    && appendFinalConsonant(result, sound)) {
                // 영어 단어 끝의 n, m, l, ng는 앞 음절의 받침으로 붙인다.
            } else {
                result.append(consonantWithoutVowel(sound));
            }
        }

        return result.isEmpty() ? word.toUpperCase(Locale.ROOT) : result.toString();
    }

    private String fixedPronunciation(String sound) {
        return switch (sound) {
            case "tion", "sion" -> "션";
            case "ture" -> "처";
            default -> null;
        };
    }

    private List<String> tokenizeSounds(String word) {
        List<String> sounds = new ArrayList<>();
        for (int index = 0; index < word.length();) {
            String remaining = word.substring(index);
            String matched = longestPrefix(
                    remaining,
                    "eigh", "ough", "tion", "sion", "ture",
                    "igh", "air", "eer", "ch", "sh", "th", "ph",
                    "ck", "ng", "qu", "ee", "ea", "oo", "oa",
                    "ou", "ow", "ai", "ay", "oi", "oy", "au",
                    "aw", "ie", "ue", "er", "ir", "ur", "ar", "or"
            );
            if (matched == null) {
                matched = String.valueOf(word.charAt(index));
            }
            sounds.add(matched);
            index += matched.length();
        }
        return sounds;
    }

    private String longestPrefix(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.startsWith(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isVowel(String sound) {
        return Set.of(
                "a", "e", "i", "o", "u", "y", "eigh", "ough",
                "igh", "air", "eer", "ee", "ea", "oo", "oa",
                "ou", "ow", "ai", "ay", "oi", "oy", "au", "aw",
                "ie", "ue", "er", "ir", "ur", "ar", "or"
        ).contains(sound);
    }

    private String syllable(String consonant, String vowel) {
        String initial = switch (consonant) {
            case "b", "v" -> "ㅂ";
            case "c", "k", "ck", "q", "qu" -> "ㅋ";
            case "d" -> "ㄷ";
            case "f", "ph" -> "ㅍ";
            case "g" -> "ㄱ";
            case "h" -> "ㅎ";
            case "j", "z" -> "ㅈ";
            case "l", "r" -> "ㄹ";
            case "m" -> "ㅁ";
            case "n", "ng" -> "ㄴ";
            case "p" -> "ㅍ";
            case "s", "sh", "th" -> "ㅅ";
            case "t" -> "ㅌ";
            case "w", "y" -> "ㅇ";
            case "ch" -> "ㅊ";
            default -> "ㅇ";
        };
        return composeInitialAndVowel(initial, vowel);
    }

    private String composeInitialAndVowel(String initial, String vowel) {
        String koreanVowel = switch (vowel) {
            case "a" -> "ㅐ";
            case "e", "air" -> "ㅔ";
            case "i", "ee", "ea", "eer", "ie", "y" -> "ㅣ";
            case "o", "oa", "or", "ough" -> "ㅗ";
            case "u", "er", "ir", "ur" -> "ㅓ";
            case "oo", "ue" -> "ㅜ";
            case "ou", "ow" -> "ㅏ";
            case "ai", "ay", "eigh" -> "ㅔ";
            case "oi", "oy" -> "ㅚ";
            case "au", "aw" -> "ㅗ";
            case "igh" -> "ㅏ";
            case "ar" -> "ㅏ";
            default -> "ㅡ";
        };
        String first = compose(initial, koreanVowel, "");
        return switch (vowel) {
            case "ou", "ow" -> first + "우";
            case "ai", "ay", "eigh" -> first + "이";
            case "oi", "oy" -> first + "이";
            case "igh" -> first + "이";
            default -> first;
        };
    }

    private String vowelWithoutOnset(String vowel) {
        return composeInitialAndVowel("ㅇ", vowel);
    }

    private boolean isAttachableFinal(String sound) {
        return Set.of("n", "m", "l", "ng").contains(sound);
    }

    private boolean appendFinalConsonant(StringBuilder result, String sound) {
        if (result.isEmpty()) {
            return false;
        }
        int lastIndex = result.length() - 1;
        char last = result.charAt(lastIndex);
        if (last < 0xAC00 || last > 0xD7A3 || (last - 0xAC00) % 28 != 0) {
            return false;
        }
        int finalIndex = switch (sound) {
            case "n" -> 4;
            case "l" -> 8;
            case "m" -> 16;
            case "ng" -> 21;
            default -> 0;
        };
        result.setCharAt(lastIndex, (char) (last + finalIndex));
        return true;
    }

    private String consonantWithoutVowel(String consonant) {
        return switch (consonant) {
            case "b", "v" -> "브";
            case "c", "k", "ck", "q", "qu", "x" -> "크";
            case "d" -> "드";
            case "f", "ph" -> "프";
            case "g" -> "그";
            case "h" -> "흐";
            case "j" -> "지";
            case "l", "r" -> "르";
            case "m" -> "므";
            case "n", "ng" -> "느";
            case "p" -> "프";
            case "s", "th" -> "스";
            case "sh" -> "시";
            case "t" -> "트";
            case "w" -> "우";
            case "y" -> "이";
            case "z" -> "즈";
            case "ch" -> "치";
            default -> "";
        };
    }

    private String compose(String initial, String vowel, String finalConsonant) {
        String initials = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
        String vowels = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
        String finals = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
        int initialIndex = initials.indexOf(initial);
        int vowelIndex = vowels.indexOf(vowel);
        int finalIndex = finals.indexOf(finalConsonant.isEmpty() ? " " : finalConsonant);
        return String.valueOf((char) (
                0xAC00 + (initialIndex * 21 + vowelIndex) * 28 + finalIndex
        ));
    }
}
