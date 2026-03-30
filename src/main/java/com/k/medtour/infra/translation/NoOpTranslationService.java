package com.k.medtour.infra.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MVP용 NoOp 번역 서비스 구현체.
 * 실제 번역 없이 빈 Map을 반환한다.
 * TODO: 외부 번역 API 연동 시 새로운 구현체로 교체
 */
public class NoOpTranslationService implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpTranslationService.class);

    @Override
    public Map<String, String> translate(String text, String fromLang, String toLang) {
        log.debug("NoOp translation called: text='{}', from={}, to={}", text, fromLang, toLang);
        return Map.of();
    }
}
