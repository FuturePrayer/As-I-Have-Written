package cn.suhoan.asihavewritten.log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final int MAX_TOKENS = 256;
    private static final int MAX_METADATA_VALUES = 32;
    private static final Pattern LATIN_OR_NUMBER = Pattern.compile("[\\p{IsAlphabetic}\\p{IsDigit}_:\\-.]{2,}");
    private static final Pattern CJK_SEQUENCE = Pattern.compile("[\\p{IsHan}]{2,}");

    public List<String> tokenize(LogIngestRequest request) {
        Set<String> tokens = new LinkedHashSet<>();
        addText(tokens, request.service());
        addText(tokens, request.environment());
        addText(tokens, request.traceId());
        addText(tokens, request.spanId());
        addText(tokens, request.message());
        addMetadata(tokens, request.metadata());
        return new ArrayList<>(tokens);
    }

    private void addMetadata(Set<String> tokens, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        int count = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (count >= MAX_METADATA_VALUES || tokens.size() >= MAX_TOKENS) {
                return;
            }
            addText(tokens, entry.getKey());
            Object value = entry.getValue();
            if (value instanceof String stringValue) {
                addText(tokens, stringValue);
                count++;
            } else if (value instanceof Number || value instanceof Boolean) {
                addText(tokens, String.valueOf(value));
                count++;
            }
        }
    }

    private void addText(Set<String> tokens, String text) {
        if (text == null || text.isBlank() || tokens.size() >= MAX_TOKENS) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        Matcher latinMatcher = LATIN_OR_NUMBER.matcher(normalized);
        while (latinMatcher.find() && tokens.size() < MAX_TOKENS) {
            tokens.add(trimToken(latinMatcher.group()));
        }
        Matcher cjkMatcher = CJK_SEQUENCE.matcher(normalized);
        while (cjkMatcher.find() && tokens.size() < MAX_TOKENS) {
            addCjkTokens(tokens, cjkMatcher.group());
        }
    }

    private void addCjkTokens(Set<String> tokens, String sequence) {
        if (sequence.length() <= 8) {
            tokens.add(sequence);
        }
        for (int i = 0; i < sequence.length() - 1 && tokens.size() < MAX_TOKENS; i++) {
            tokens.add(sequence.substring(i, i + 2));
        }
    }

    private String trimToken(String token) {
        return token.replaceAll("(^[-_:.]+|[-_:.]+$)", "");
    }
}
