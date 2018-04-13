package com.kangaroo.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by woonill on 20/12/2016.
 */
public final class AntPathMatcher {

    /**
     * Default path separator: "/"
     */
    public static final String DEFAULT_PATH_SEPARATOR = "/";

    private static final int CACHE_TURNOFF_THRESHOLD = 65536;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");


    private String pathSeparator;

    private PathSeparatorPatternCache pathSeparatorPatternCache;

    private boolean trimTokens = true;

    private volatile Boolean cachePatterns;

    private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<String, String[]>(256);

    final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<String, AntPathStringMatcher>(256);


    /**
     * Create a new instance with the {@link #DEFAULT_PATH_SEPARATOR}.
     */
    public AntPathMatcher() {
        this.pathSeparator = DEFAULT_PATH_SEPARATOR;
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR);
    }

    /**
     * A convenience alternative constructor to use with a custom path separator.
     *
     * @param pathSeparator the path separator to use, must not be {@code null}.
     * @since 4.1
     */
    public AntPathMatcher(String pathSeparator) {
        Assert.notNull(pathSeparator, "'pathSeparator' is required");
        this.pathSeparator = pathSeparator;
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
    }


    /**
     * Set the path separator to use for pattern parsing.
     * Default is "/", as in Ant.
     */
    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
    }

    /**
     * Specify whether to trim tokenized paths and patterns.
     * Default is {@code true}.
     */
    public void setTrimTokens(boolean trimTokens) {
        this.trimTokens = trimTokens;
    }

    /**
     * Specify whether to cache parsed pattern metadata for patterns passed
     * into this matcher's {@link #match} method. A value of {@code true}
     * activates an unlimited pattern cache; a value of {@code false} turns
     * the pattern cache off completely.
     * <p>Default is for the cache to be on, but with the variant to automatically
     * turn it off when encountering too many patterns to cache at runtime
     * (the threshold is 65536), assuming that arbitrary permutations of patterns
     * are coming in, with little chance for encountering a reoccurring pattern.
     *
     * @see #getStringMatcher(String)
     */
    public void setCachePatterns(boolean cachePatterns) {
        this.cachePatterns = cachePatterns;
    }

    private void deactivatePatternCache() {
        this.cachePatterns = false;
        this.tokenizedPatternCache.clear();
        this.stringMatcherCache.clear();
    }


    public boolean isPattern(String path) {
        return (path.indexOf('*') != -1 || path.indexOf('?') != -1);
    }

    public boolean match(String pattern, String path) {
        return doMatch(pattern, path, true, null);
    }

    public boolean matchStart(String pattern, String path) {
        return doMatch(pattern, path, false, null);
    }

    /**
     * Actually match the given {@code path} against the given {@code pattern}.
     *
     * @param pattern   the pattern to match against
     * @param path      the path String to test
     * @param fullMatch whether a full pattern match is required (else a pattern match
     *                  as far as the given base path goes is sufficient)
     * @return {@code true} if the supplied {@code path} matched, {@code false} if it didn't
     */
    protected boolean doMatch(String pattern, String path, boolean fullMatch, Map<String, String> uriTemplateVariables) {
        if (path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
            return false;
        }

        String[] pattDirs = tokenizePattern(pattern);
        String[] pathDirs = tokenizePath(path);

        int pattIdxStart = 0;
        int pattIdxEnd = pattDirs.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathDirs.length - 1;

        // Match all elements up to the first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattDirs[pattIdxStart];
            if ("**".equals(pattDir)) {
                break;
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }

        if (pathIdxStart > pathIdxEnd) {
            // Path is exhausted, only match if rest of pattern is * or **'s
            if (pattIdxStart > pattIdxEnd) {
                return (pattern.endsWith(this.pathSeparator) ? path.endsWith(this.pathSeparator) :
                        !path.endsWith(this.pathSeparator));
            }
            if (!fullMatch) {
                return true;
            }
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        } else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }

        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattDirs[pattIdxEnd];
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - pattIdxStart - 1);
            int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;

            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = pattDirs[pattIdxStart + j + 1];
                    String subStr = pathDirs[pathIdxStart + i + j];
                    if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }

        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!pattDirs[i].equals("**")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tokenize the given path pattern into parts, based on this matcher's settings.
     * <p>Performs caching based on {@link #setCachePatterns}, delegating to
     * {@link #tokenizePath(String)} for the actual tokenization algorithm.
     *
     * @param pattern the pattern to tokenize
     * @return the tokenized pattern parts
     */
    protected String[] tokenizePattern(String pattern) {
        String[] tokenized = null;
        Boolean cachePatterns = this.cachePatterns;
        if (cachePatterns == null || cachePatterns.booleanValue()) {
            tokenized = this.tokenizedPatternCache.get(pattern);
        }
        if (tokenized == null) {
            tokenized = tokenizePath(pattern);
            if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
                // Try to adapt to the runtime situation that we're encountering:
                // There are obviously too many different patterns coming in here...
                // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                deactivatePatternCache();
                return tokenized;
            }
            if (cachePatterns == null || cachePatterns.booleanValue()) {
                this.tokenizedPatternCache.put(pattern, tokenized);
            }
        }
        return tokenized;
    }

    /**
     * Tokenize the given path String into parts, based on this matcher's settings.
     *
     * @param path the path to tokenize
     * @return the tokenized path parts
     */
    protected String[] tokenizePath(String path) {
        return StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
    }

    /**
     * Tests whether or not a string matches against a pattern.
     *
     * @param pattern the pattern to match against (never {@code null})
     * @param str     the String which must be matched against the pattern (never {@code null})
     * @return {@code true} if the string matches against the pattern, or {@code false} otherwise
     */
    private boolean matchStrings(String pattern, String str, Map<String, String> uriTemplateVariables) {
        return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
    }

    /**
     * Build or retrieve an {@link AntPathStringMatcher} for the given pattern.
     * <p>The default implementation checks this AntPathMatcher's internal cache
     * (see {@link #setCachePatterns}), creating a new AntPathStringMatcher instance
     * if no cached copy is found.
     * When encountering too many patterns to cache at runtime (the threshold is 65536),
     * it turns the default cache off, assuming that arbitrary permutations of patterns
     * are coming in, with little chance for encountering a reoccurring pattern.
     * <p>This method may get overridden to implement a custom cache strategy.
     *
     * @param pattern the pattern to match against (never {@code null})
     * @return a corresponding AntPathStringMatcher (never {@code null})
     * @see #setCachePatterns
     */
    protected AntPathStringMatcher getStringMatcher(String pattern) {
        AntPathStringMatcher matcher = null;
        Boolean cachePatterns = this.cachePatterns;
        if (cachePatterns == null || cachePatterns.booleanValue()) {
            matcher = this.stringMatcherCache.get(pattern);
        }
        if (matcher == null) {
            matcher = new AntPathStringMatcher(pattern);
            if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
                // Try to adapt to the runtime situation that we're encountering:
                // There are obviously too many different patterns coming in here...
                // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                deactivatePatternCache();
                return matcher;
            }
            if (cachePatterns == null || cachePatterns.booleanValue()) {
                this.stringMatcherCache.put(pattern, matcher);
            }
        }
        return matcher;
    }

    /**
     * Given a pattern and a full path, determine the pattern-mapped part. <p>For example: <ul>
     * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html} -> ''</li>
     * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit} -> '{@code cvs/commit}'</li>
     * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html} -> '{@code commit.html}'</li>
     * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit} -> '{@code cvs/commit}'</li>
     * <li>'{@code /docs/**\/*.html}' and '{@code /docs/cvs/commit.html} -> '{@code cvs/commit.html}'</li>
     * <li>'{@code /*.html}' and '{@code /docs/cvs/commit.html} -> '{@code docs/cvs/commit.html}'</li>
     * <li>'{@code *.html}' and '{@code /docs/cvs/commit.html} -> '{@code /docs/cvs/commit.html}'</li>
     * <li>'{@code *}' and '{@code /docs/cvs/commit.html} -> '{@code /docs/cvs/commit.html}'</li> </ul>
     * <p>Assumes that {@link #match} returns {@code true} for '{@code pattern}' and '{@code path}', but
     * does <strong>not</strong> enforce this.
     */
    public String extractPathWithinPattern(String pattern, String path) {
        String[] patternParts = StringUtils.tokenizeToStringArray(pattern, this.pathSeparator, this.trimTokens, true);
        String[] pathParts = StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
        StringBuilder builder = new StringBuilder();
        boolean pathStarted = false;

        for (int segment = 0; segment < patternParts.length; segment++) {
            String patternPart = patternParts[segment];
            if (patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
                for (; segment < pathParts.length; segment++) {
                    if (pathStarted || (segment == 0 && !pattern.startsWith(this.pathSeparator))) {
                        builder.append(this.pathSeparator);
                    }
                    builder.append(pathParts[segment]);
                    pathStarted = true;
                }
            }
        }

        return builder.toString();
    }

    public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
        Map<String, String> variables = new LinkedHashMap<String, String>();
        boolean result = doMatch(pattern, path, true, variables);
        Assert.state(result, "Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
        return variables;
    }

    /**
     * Combines two patterns into a new pattern that is returned.
     * <p>This implementation simply concatenates the two patterns, unless the first pattern
     * contains a file extension match (such as {@code *.html}. In that case, the second pattern
     * should be included in the first, or an {@code IllegalArgumentException} is thrown.
     * <p>For example: <table>
     * <tr><th>Pattern 1</th><th>Pattern 2</th><th>Result</th></tr> <tr><td>/hotels</td><td>{@code
     * null}</td><td>/hotels</td></tr> <tr><td>{@code null}</td><td>/hotels</td><td>/hotels</td></tr>
     * <tr><td>/hotels</td><td>/bookings</td><td>/hotels/bookings</td></tr> <tr><td>/hotels</td><td>bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels/*</td><td>/bookings</td><td>/hotels/bookings</td></tr> <tr><td>/hotels/&#42;&#42;</td><td>/bookings</td><td>/hotels/&#42;&#42;/bookings</td></tr>
     * <tr><td>/hotels</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr> <tr><td>/hotels/*</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
     * <tr><td>/hotels/&#42;&#42;</td><td>{hotel}</td><td>/hotels/&#42;&#42;/{hotel}</td></tr>
     * <tr><td>/*.html</td><td>/hotels.html</td><td>/hotels.html</td></tr> <tr><td>/*.html</td><td>/hotels</td><td>/hotels.html</td></tr>
     * <tr><td>/*.html</td><td>/*.txt</td><td>IllegalArgumentException</td></tr> </table>
     *
     * @param pattern1 the first pattern
     * @param pattern2 the second pattern
     * @return the combination of the two patterns
     * @throws IllegalArgumentException when the two patterns cannot be combined
     */
    public String combine(String pattern1, String pattern2) {
        if (!StringUtils.hasText(pattern1) && !StringUtils.hasText(pattern2)) {
            return "";
        }
        if (!StringUtils.hasText(pattern1)) {
            return pattern2;
        }
        if (!StringUtils.hasText(pattern2)) {
            return pattern1;
        }

        boolean pattern1ContainsUriVar = pattern1.indexOf('{') != -1;
        if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
            // /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
            // However /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
            return pattern2;
        }

        // /hotels/* + /booking -> /hotels/booking
        // /hotels/* + booking -> /hotels/booking
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
            return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
        }

        // /hotels/** + /booking -> /hotels/**/booking
        // /hotels/** + booking -> /hotels/**/booking
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
            return concat(pattern1, pattern2);
        }

        int starDotPos1 = pattern1.indexOf("*.");
        if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
            // simply concatenate the two patterns
            return concat(pattern1, pattern2);
        }
        String extension1 = pattern1.substring(starDotPos1 + 1);
        int dotPos2 = pattern2.indexOf('.');
        String fileName2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
        String extension2 = (dotPos2 == -1 ? "" : pattern2.substring(dotPos2));
        String extension = extension1.startsWith("*") ? extension2 : extension1;
        return fileName2 + extension;
    }

    private String concat(String path1, String path2) {
        if (path1.endsWith(this.pathSeparator) || path2.startsWith(this.pathSeparator)) {
            return path1 + path2;
        }
        return path1 + this.pathSeparator + path2;
    }

    /**
     * Given a full path, returns a {@link Comparator} suitable for sorting patterns in order of explicitness.
     * <p>The returned {@code Comparator} will {@linkplain Collections#sort(List,
     * Comparator) sort} a list so that more specific patterns (without uri templates or wild cards) come before
     * generic patterns. So given a list with the following patterns: <ol> <li>{@code /hotels/new}</li>
     * <li>{@code /hotels/{hotel}}</li> <li>{@code /hotels/*}</li> </ol> the returned comparator will sort this
     * list so that the order will be as indicated.
     * <p>The full path given as parameter is used to test for exact matches. So when the given path is {@code /hotels/2},
     * the pattern {@code /hotels/2} will be sorted before {@code /hotels/1}.
     *
     * @param path the full path to use for comparison
     * @return a comparator capable of sorting patterns in order of explicitness
     */
    public Comparator<String> getPatternComparator(String path) {
        return new AntPatternComparator(path);
    }


    /**
     * Tests whether or not a string matches against a pattern via a {@link Pattern}.
     * <p>The pattern may contain special characters: '*' means zero or more characters; '?' means one and
     * only one character; '{' and '}' indicate a URI template pattern. For example <tt>/users/{user}</tt>.
     */
    protected static class AntPathStringMatcher {

        private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

        private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

        private final Pattern pattern;

        private final List<String> variableNames = new LinkedList<String>();

        public AntPathStringMatcher(String pattern) {
            StringBuilder patternBuilder = new StringBuilder();
            Matcher m = GLOB_PATTERN.matcher(pattern);
            int end = 0;
            while (m.find()) {
                patternBuilder.append(quote(pattern, end, m.start()));
                String match = m.group();
                if ("?".equals(match)) {
                    patternBuilder.append('.');
                } else if ("*".equals(match)) {
                    patternBuilder.append(".*");
                } else if (match.startsWith("{") && match.endsWith("}")) {
                    int colonIdx = match.indexOf(':');
                    if (colonIdx == -1) {
                        patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                        this.variableNames.add(m.group(1));
                    } else {
                        String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                        patternBuilder.append('(');
                        patternBuilder.append(variablePattern);
                        patternBuilder.append(')');
                        String variableName = match.substring(1, colonIdx);
                        this.variableNames.add(variableName);
                    }
                }
                end = m.end();
            }
            patternBuilder.append(quote(pattern, end, pattern.length()));
            this.pattern = Pattern.compile(patternBuilder.toString());
        }

        private String quote(String s, int start, int end) {
            if (start == end) {
                return "";
            }
            return Pattern.quote(s.substring(start, end));
        }

        /**
         * Main entry point.
         *
         * @return {@code true} if the string matches against the pattern, or {@code false} otherwise.
         */
        public boolean matchStrings(String str, Map<String, String> uriTemplateVariables) {
            Matcher matcher = this.pattern.matcher(str);
            if (matcher.matches()) {
                if (uriTemplateVariables != null) {
                    // SPR-8455
                    Assert.isTrue(this.variableNames.size() == matcher.groupCount(),
                            "The number of capturing groups in the pattern segment " + this.pattern +
                                    " does not match the number of URI template variables it defines, which can occur if " +
                                    " capturing groups are used in a URI template regex. Use non-capturing groups instead.");
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        String name = this.variableNames.get(i - 1);
                        String value = matcher.group(i);
                        uriTemplateVariables.put(name, value);
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }


    /**
     * The default {@link Comparator} implementation returned by
     * {@link #getPatternComparator(String)}.
     * <p>In order, the most "generic" pattern is determined by the following:
     * <ul>
     * <li>if it's null or a capture all pattern (i.e. it is equal to "/**")</li>
     * <li>if the other pattern is an actual match</li>
     * <li>if it's a catch-all pattern (i.e. it ends with "**"</li>
     * <li>if it's got more "*" than the other pattern</li>
     * <li>if it's got more "{foo}" than the other pattern</li>
     * <li>if it's shorter than the other pattern</li>
     * </ul>
     */
    protected static class AntPatternComparator implements Comparator<String> {

        private final String path;

        public AntPatternComparator(String path) {
            this.path = path;
        }

        /**
         * Compare two patterns to determine which should match first, i.e. which
         * is the most specific regarding the current path.
         *
         * @return a negative integer, zero, or a positive integer as pattern1 is
         * more specific, equally specific, or less specific than pattern2.
         */
        @Override
        public int compare(String pattern1, String pattern2) {
            PatternInfo info1 = new PatternInfo(pattern1);
            PatternInfo info2 = new PatternInfo(pattern2);

            if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
                return 0;
            } else if (info1.isLeastSpecific()) {
                return 1;
            } else if (info2.isLeastSpecific()) {
                return -1;
            }

            boolean pattern1EqualsPath = pattern1.equals(path);
            boolean pattern2EqualsPath = pattern2.equals(path);
            if (pattern1EqualsPath && pattern2EqualsPath) {
                return 0;
            } else if (pattern1EqualsPath) {
                return -1;
            } else if (pattern2EqualsPath) {
                return 1;
            }

            if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
                return 1;
            } else if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
                return -1;
            }

            if (info1.getTotalCount() != info2.getTotalCount()) {
                return info1.getTotalCount() - info2.getTotalCount();
            }

            if (info1.getLength() != info2.getLength()) {
                return info2.getLength() - info1.getLength();
            }

            if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
                return -1;
            } else if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
                return 1;
            }

            if (info1.getUriVars() < info2.getUriVars()) {
                return -1;
            } else if (info2.getUriVars() < info1.getUriVars()) {
                return 1;
            }

            return 0;
        }


        /**
         * Value class that holds information about the pattern, e.g. number of
         * occurrences of "*", "**", and "{" pattern elements.
         */
        private static class PatternInfo {

            private final String pattern;

            private int uriVars;

            private int singleWildcards;

            private int doubleWildcards;

            private boolean catchAllPattern;

            private boolean prefixPattern;

            private Integer length;

            public PatternInfo(String pattern) {
                this.pattern = pattern;
                if (this.pattern != null) {
                    initCounters();
                    this.catchAllPattern = this.pattern.equals("/**");
                    this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith("/**");
                }
                if (this.uriVars == 0) {
                    this.length = (this.pattern != null ? this.pattern.length() : 0);
                }
            }

            protected void initCounters() {
                int pos = 0;
                while (pos < this.pattern.length()) {
                    if (this.pattern.charAt(pos) == '{') {
                        this.uriVars++;
                        pos++;
                    } else if (this.pattern.charAt(pos) == '*') {
                        if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
                            this.doubleWildcards++;
                            pos += 2;
                        } else if (!this.pattern.substring(pos - 1).equals(".*")) {
                            this.singleWildcards++;
                            pos++;
                        } else {
                            pos++;
                        }
                    } else {
                        pos++;
                    }
                }
            }

            public int getUriVars() {
                return this.uriVars;
            }

            public int getSingleWildcards() {
                return this.singleWildcards;
            }

            public int getDoubleWildcards() {
                return this.doubleWildcards;
            }

            public boolean isLeastSpecific() {
                return (this.pattern == null || this.catchAllPattern);
            }

            public boolean isPrefixPattern() {
                return this.prefixPattern;
            }

            public int getTotalCount() {
                return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
            }

            /**
             * Returns the length of the given pattern, where template variables are considered to be 1 long.
             */
            public int getLength() {
                if (this.length == null) {
                    this.length = VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length();
                }
                return this.length;
            }
        }
    }


    /**
     * A simple cache for patterns that depend on the configured path separator.
     */
    private static class PathSeparatorPatternCache {

        private final String endsOnWildCard;

        private final String endsOnDoubleWildCard;

        public PathSeparatorPatternCache(String pathSeparator) {
            this.endsOnWildCard = pathSeparator + "*";
            this.endsOnDoubleWildCard = pathSeparator + "**";
        }

        public String getEndsOnWildCard() {
            return this.endsOnWildCard;
        }

        public String getEndsOnDoubleWildCard() {
            return this.endsOnDoubleWildCard;
        }
    }

    /**
     * Created by woonill on 20/12/2016.
     */
    abstract static class Assert {

        /**
         * Assert a boolean expression, throwing {@code IllegalArgumentException}
         * if the test result is {@code false}.
         * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
         *
         * @param expression a boolean expression
         * @param message    the exception message to use if the assertion fails
         * @throws IllegalArgumentException if expression is {@code false}
         */
        public static void isTrue(boolean expression, String message) {
            if (!expression) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert a boolean expression, throwing {@code IllegalArgumentException}
         * if the test result is {@code false}.
         * <pre class="code">Assert.isTrue(i &gt; 0);</pre>
         *
         * @param expression a boolean expression
         * @throws IllegalArgumentException if expression is {@code false}
         */
        public static void isTrue(boolean expression) {
            isTrue(expression, "[Assertion failed] - this expression must be true");
        }

        /**
         * Assert that an object is {@code null} .
         * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
         *
         * @param object  the object to check
         * @param message the exception message to use if the assertion fails
         * @throws IllegalArgumentException if the object is not {@code null}
         */
        public static void isNull(Object object, String message) {
            if (object != null) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that an object is {@code null} .
         * <pre class="code">Assert.isNull(value);</pre>
         *
         * @param object the object to check
         * @throws IllegalArgumentException if the object is not {@code null}
         */
        public static void isNull(Object object) {
            isNull(object, "[Assertion failed] - the object argument must be null");
        }

        /**
         * Assert that an object is not {@code null} .
         * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
         *
         * @param object  the object to check
         * @param message the exception message to use if the assertion fails
         * @throws IllegalArgumentException if the object is {@code null}
         */
        public static void notNull(Object object, String message) {
            if (object == null) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that an object is not {@code null} .
         * <pre class="code">Assert.notNull(clazz);</pre>
         *
         * @param object the object to check
         * @throws IllegalArgumentException if the object is {@code null}
         */
        public static void notNull(Object object) {
            notNull(object, "[Assertion failed] - this argument is required; it must not be null");
        }

        /**
         * Assert that the given String is not empty; that is,
         * it must not be {@code null} and not the empty String.
         * <pre class="code">Assert.hasLength(name, "Name must not be empty");</pre>
         *
         * @param text    the String to check
         * @param message the exception message to use if the assertion fails
         * @see StringUtils#hasLength
         */
        public static void hasLength(String text, String message) {
            if (!StringUtils.hasLength(text)) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that the given String is not empty; that is,
         * it must not be {@code null} and not the empty String.
         * <pre class="code">Assert.hasLength(name);</pre>
         *
         * @param text the String to check
         * @see StringUtils#hasLength
         */
        public static void hasLength(String text) {
            hasLength(text,
                    "[Assertion failed] - this String argument must have length; it must not be null or empty");
        }

        /**
         * Assert that the given String has valid text content; that is, it must not
         * be {@code null} and must contain at least one non-whitespace character.
         * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
         *
         * @param text    the String to check
         * @param message the exception message to use if the assertion fails
         * @see StringUtils#hasText
         */
        public static void hasText(String text, String message) {
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that the given String has valid text content; that is, it must not
         * be {@code null} and must contain at least one non-whitespace character.
         * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
         *
         * @param text the String to check
         * @see StringUtils#hasText
         */
        public static void hasText(String text) {
            hasText(text,
                    "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
        }

        /**
         * Assert that the given text does not contain the given substring.
         * <pre class="code">Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");</pre>
         *
         * @param textToSearch the text to search
         * @param substring    the substring to find within the text
         * @param message      the exception message to use if the assertion fails
         */
        public static void doesNotContain(String textToSearch, String substring, String message) {
            if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
                    textToSearch.contains(substring)) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that the given text does not contain the given substring.
         * <pre class="code">Assert.doesNotContain(name, "rod");</pre>
         *
         * @param textToSearch the text to search
         * @param substring    the substring to find within the text
         */
        public static void doesNotContain(String textToSearch, String substring) {
            doesNotContain(textToSearch, substring,
                    "[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
        }


        /**
         * Assert that an array has elements; that is, it must not be
         * {@code null} and must have at least one element.
         * <pre class="code">Assert.notEmpty(array, "The array must have elements");</pre>
         *
         * @param array   the array to check
         * @param message the exception message to use if the assertion fails
         * @throws IllegalArgumentException if the object array is {@code null} or has no elements
         */
        public static void notEmpty(Object[] array, String message) {
            if (ObjectUtil.isEmpty(array)) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that an array has elements; that is, it must not be
         * {@code null} and must have at least one element.
         * <pre class="code">Assert.notEmpty(array);</pre>
         *
         * @param array the array to check
         * @throws IllegalArgumentException if the object array is {@code null} or has no elements
         */
        public static void notEmpty(Object[] array) {
            notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
        }

        /**
         * Assert that an array has no null elements.
         * Note: Does not complain if the array is empty!
         * <pre class="code">Assert.noNullElements(array, "The array must have non-null elements");</pre>
         *
         * @param array   the array to check
         * @param message the exception message to use if the assertion fails
         * @throws IllegalArgumentException if the object array contains a {@code null} element
         */
        public static void noNullElements(Object[] array, String message) {
            if (array != null) {
                for (Object element : array) {
                    if (element == null) {
                        throw new IllegalArgumentException(message);
                    }
                }
            }
        }

        /**
         * Assert that an array has no null elements.
         * Note: Does not complain if the array is empty!
         * <pre class="code">Assert.noNullElements(array);</pre>
         *
         * @param array the array to check
         * @throws IllegalArgumentException if the object array contains a {@code null} element
         */
        public static void noNullElements(Object[] array) {
            noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
        }

        /**
         * Assert that a collection has elements; that is, it must not be
         * {@code null} and must have at least one element.
         * <pre class="code">Assert.notEmpty(collection, "Collection must have elements");</pre>
         *
         * @param collection the collection to check
         * @param message    the exception message to use if the assertion fails
         * @throws IllegalArgumentException if the collection is {@code null} or has no elements
         */
        public static void notEmpty(Collection<?> collection, String message) {
            if (Assert.isCollectionEmpty(collection)) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that a collection has elements; that is, it must not be
         * {@code null} and must have at least one element.
         * <pre class="code">Assert.notEmpty(collection, "Collection must have elements");</pre>
         *
         * @param collection the collection to check
         * @throws IllegalArgumentException if the collection is {@code null} or has no elements
         */
        public static void notEmpty(Collection<?> collection) {
            notEmpty(collection,
                    "[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
        }

        /**
         * Assert that a Map has entries; that is, it must not be {@code null}
         * and must have at least one entry.
         * <pre class="code">Assert.notEmpty(map, "Map must have entries");</pre>
         *
         * @param map     the map to check
         * @param message the exception message to use if the assertion fails
         * @throws IllegalArgumentException if the map is {@code null} or has no entries
         */
        public static void notEmpty(Map<?, ?> map, String message) {
            if (Assert.isEmptyMap(map)) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Assert that a Map has entries; that is, it must not be {@code null}
         * and must have at least one entry.
         * <pre class="code">Assert.notEmpty(map);</pre>
         *
         * @param map the map to check
         * @throws IllegalArgumentException if the map is {@code null} or has no entries
         */
        public static void notEmpty(Map<?, ?> map) {
            notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
        }


        /**
         * Assert that the provided object is an instance of the provided class.
         * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
         *
         * @param clazz the required class
         * @param obj   the object to check
         * @throws IllegalArgumentException if the object is not an instance of clazz
         * @see Class#isInstance
         */
        public static void isInstanceOf(Class<?> clazz, Object obj) {
            isInstanceOf(clazz, obj, "");
        }

        /**
         * Assert that the provided object is an instance of the provided class.
         * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
         *
         * @param type    the type to check against
         * @param obj     the object to check
         * @param message a message which will be prepended to the message produced by
         *                the function itself, and which may be used to provide context. It should
         *                normally end in a ": " or ". " so that the function generate message looks
         *                ok when prepended to it.
         * @throws IllegalArgumentException if the object is not an instance of clazz
         * @see Class#isInstance
         */
        public static void isInstanceOf(Class<?> type, Object obj, String message) {
            notNull(type, "Type to check against must not be null");
            if (!type.isInstance(obj)) {
                throw new IllegalArgumentException(
                        (StringUtils.hasLength(message) ? message + " " : "") +
                                "Object of class [" + (obj != null ? obj.getClass().getName() : "null") +
                                "] must be an instance of " + type);
            }
        }

        /**
         * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
         * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
         *
         * @param superType the super type to check
         * @param subType   the sub type to check
         * @throws IllegalArgumentException if the classes are not assignable
         */
        public static void isAssignable(Class<?> superType, Class<?> subType) {
            isAssignable(superType, subType, "");
        }

        /**
         * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
         * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
         *
         * @param superType the super type to check against
         * @param subType   the sub type to check
         * @param message   a message which will be prepended to the message produced by
         *                  the function itself, and which may be used to provide context. It should
         *                  normally end in a ": " or ". " so that the function generate message looks
         *                  ok when prepended to it.
         * @throws IllegalArgumentException if the classes are not assignable
         */
        public static void isAssignable(Class<?> superType, Class<?> subType, String message) {
            notNull(superType, "Type to check against must not be null");
            if (subType == null || !superType.isAssignableFrom(subType)) {
                throw new IllegalArgumentException(message + subType + " is not assignable to " + superType);
            }
        }


        /**
         * Assert a boolean expression, throwing {@code IllegalStateException}
         * if the test result is {@code false}. Call isTrue if you wish to
         * throw IllegalArgumentException on an assertion failure.
         * <pre class="code">Assert.state(id == null, "The id property must not already be initialized");</pre>
         *
         * @param expression a boolean expression
         * @param message    the exception message to use if the assertion fails
         * @throws IllegalStateException if expression is {@code false}
         */
        public static void state(boolean expression, String message) {
            if (!expression) {
                throw new IllegalStateException(message);
            }
        }

        /**
         * Assert a boolean expression, throwing {@link IllegalStateException}
         * if the test result is {@code false}.
         * <p>Call {@link #isTrue(boolean)} if you wish to
         * throw {@link IllegalArgumentException} on an assertion failure.
         * <pre class="code">Assert.state(id == null);</pre>
         *
         * @param expression a boolean expression
         * @throws IllegalStateException if the supplied expression is {@code false}
         */
        public static void state(boolean expression) {
            state(expression, "[Assertion failed] - this state invariant must be true");
        }


        public static boolean isCollectionEmpty(Collection<?> collection) {
            return (collection == null || collection.isEmpty());
        }

        public static boolean isEmptyMap(Map<?, ?> map) {
            return (map == null || map.isEmpty());
        }

    }

    /**
     * Created by woonill on 20/12/2016.
     */
    static class StringUtils {


        public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

            if (str == null) {
                return null;
            }
            StringTokenizer st = new StringTokenizer(str, delimiters);
            List<String> tokens = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (trimTokens) {
                    token = token.trim();
                }
                if (!ignoreEmptyTokens || token.length() > 0) {
                    tokens.add(token);
                }
            }
            return toStringArray(tokens);
        }

        private static String[] toStringArray(Collection<String> collection) {
            if (collection == null) {
                return null;
            }
            return collection.toArray(new String[collection.size()]);
        }

        public static boolean hasText(String str) {
            return hasText((CharSequence) str);
        }

        private static boolean hasText(CharSequence str) {
            if (!hasLength(str)) {
                return false;
            }
            int strLen = str.length();
            for (int i = 0; i < strLen; i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }
            return false;
        }

        public static boolean hasLength(CharSequence str) {
            return (str != null && str.length() > 0);
        }
    }
}
