package com.julensserver.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>JULens API</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                            padding: 40px;
                            line-height: 1.6;
                        }
                        a {
                            display: inline-block;
                            padding: 12px 18px;
                            border: 1px solid #333;
                            border-radius: 8px;
                            text-decoration: none;
                            color: #111;
                            font-weight: 600;
                        }
                    </style>
                </head>
                <body>
                    <h1>JULens API Server</h1>
                    <p>서버가 정상 실행 중입니다.</p>
                    <a href="/swagger-ui/index.html">Swagger UI 열기</a>
                </body>
                </html>
                """;
    }
}