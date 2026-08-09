package com.julensserver.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "잘못된 입력값입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "이메일 또는 비밀번호가 올바르지 않습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시물을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND","댓글을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "POST_ACCESS_DENIED", "게시글을 수정하거나 삭제할 권한이 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMENT_ACCESS_DENIED", "댓글을 수정하거나 삭제할 권한이 없습니다."),
    POST_ALREADY_LIKED(HttpStatus.CONFLICT, "POST_ALREADY_LIKED", "게시물에 이미 좋아요를 눌렀습니다."),
    POST_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_LIKE_NOT_FOUND", "게시물에 좋아요를 누르지 않았습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND", "종목을 찾을 수 없습니다."),
    USER_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_STOCK_NOT_FOUND", "관심종목에서 해당 종목을 찾을 수 없습니다."),
    USER_STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_STOCK_ALREADY_EXISTS", "이미 관심종목에 등록된 종목입니다."),
    LENS_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "LENS_ANALYSIS_NOT_FOUND", "완료된 분석 결과를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
