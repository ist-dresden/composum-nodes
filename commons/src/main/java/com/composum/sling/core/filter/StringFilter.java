package com.composum.sling.core.filter;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A StringFilter is useful to describe a general way to define scopes in a set of objects by key values.
 * Such a filter accepts only values which are matching to patterns of regular expressions.
 * These filters can be combined in filter sets with various combination rules.
 */
public interface StringFilter {

    /**
     * the core function of a filters says: this value is appropriate or not
     *
     * @param value the string value
     * @return 'true' if the value matches
     */
    boolean accept(String value);

    /**
     * This is a hint for filter sets and signals that
     * the filter primary restricts values (is a 'blacklist) or not
     *
     * @return 'true' if this filter excludes objects
     */
    boolean isRestriction();

    /**
     * to build a rebuildable string view of the filter
     */
    void toString(StringBuilder builder);

    /** the predefined filter instance which accepts each string value */
    StringFilter ALL = new All();

    /**
     * the 'all enabled' implementation: filters nothing, each value is appropriate
     */
    class All implements StringFilter {

        @Override
        public boolean accept(String value) {
            return true;
        }

        @Override
        public boolean isRestriction() {
            return false;
        }

        /**
         * Returns the string representation of the filter itself [All'']
         */
        @Override
        public void toString(StringBuilder builder) {
            builder.append("All''");
        }
    }

    /**
     * the abstract base for filters which are using regular expression patterns
     */
    abstract class FilterBase implements StringFilter {

        public List<String> getFiltered(Iterable<String> values) {
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (accept(value)) {
                    result.add(value);
                }
            }
            return result;
        }

        public String[] getFiltered(String... values) {
            List<String> result = new ArrayList<>();
            for (String value : values) {
                if (accept(value)) {
                    result.add(value);
                }
            }
            return result.toArray(new String[0]);
        }
    }

    /**
     * the abstract base for filters which are using regular expression patterns
     */
    abstract class PatternList extends FilterBase {

        /** such a filter uses a list of patterns to implement the filter function */
        protected List<Pattern> patterns;

        /**
         * The constructor which builds a pattern list by one single string
         * with a ',' separated list of regular expression
         *
         * @param values all expression in one string separated by ','
         */
        public PatternList(String values) {
            this(patterns(StringUtils.split(values, ",")));
        }

        /**
         * The constructor with an array of regular expressions each represented by a string value
         *
         * @param values the array of regular expression strings
         */
        public PatternList(String[] values) {
            this(patterns(values));
        }

        /**
         * The constructor with an array of pre compiled regular expressions
         *
         * @param patterns the array of regular expression patterns
         */
        public PatternList(Pattern[] patterns) {
            this.patterns = Arrays.asList(patterns);
        }

        /**
         * The constructor with an array of pre compiled regular expressions
         *
         * @param patterns the array of regular expression patterns
         */
        public PatternList(List<Pattern> patterns) {
            this.patterns = patterns;
        }

        /**
         * Returns the list of patterns of the filter set.
         */
        public List<Pattern> getPatterns() {
            return patterns;
        }

        /**
         * The constructor function to generate the regular expression patterns from string values.
         *
         * @param values the set of string values which are regular expressions
         * @return the pattern array
         */
        protected static List<Pattern> patterns(String[] values) {
            List<Pattern> patterns = new ArrayList<>();
            for (String value : values) {
                patterns.add(Pattern.compile(value));
            }
            return patterns;
        }

        /**
         * The constructor function to generate the regular expression patterns from string values.
         *
         * @param values the set of string values which are regular expressions
         * @return the pattern array
         */
        protected static List<Pattern> patterns(List<String> values) {
            List<Pattern> patterns = new ArrayList<>();
            for (String value : values) {
                patterns.add(Pattern.compile(value));
            }
            return patterns;
        }

        /**
         * Returns the string representation of the filter itself ['filter', ...)]
         */
        @Override
        public void toString(StringBuilder builder) {
            for (int i = 0; i < patterns.size(); ) {
                builder.append(patterns.get(i).pattern());
                if (++i < patterns.size()) {
                    builder.append(',');
                }
            }
        }
    }

    /**
     * The 'WhiteList' is a pattern based filter which accepts all values that contain a substring matching one of its patterns
     * (see {@link Matcher#find()}).
     */
    class WhiteList extends PatternList {

        /**
         * @see PatternList
         */
        public WhiteList(String values) {
            super(values);
        }

        /**
         * @see PatternList
         */
        public WhiteList(String... values) {
            super(values);
        }

        /**
         * @see PatternList
         */
        public WhiteList(Pattern[] patterns) {
            super(patterns);
        }

        /**
         * @see PatternList
         */
        public WhiteList(List<Pattern> patterns) {
            super(patterns);
        }

        /**
         * returns 'true' if one pattern matches
         */
        @Override
        public boolean accept(String value) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).find()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * a 'Whitelist' is never a restriction
         */
        @Override
        public boolean isRestriction() {
            return false;
        }

        /**
         * Returns the string representation of the filter itself [+'filter']
         */
        @Override
        public void toString(StringBuilder builder) {
            builder.append("+'");
            super.toString(builder);
            builder.append("'");
        }
    }

    /**
     * The 'BlackList' is a pattern based filter which accepts all values NOT containing a substring matching to one of its patterns
     * (see {@link Matcher#find()}).
     */
    class BlackList extends PatternList {

        /**
         * @see PatternList
         */
        public BlackList(String values) {
            super(values);
        }

        /**
         * @see PatternList
         */
        public BlackList(String... values) {
            super(values);
        }

        /**
         * @see PatternList
         */
        public BlackList(Pattern[] patterns) {
            super(patterns);
        }

        /**
         * @see PatternList
         */
        public BlackList(List<Pattern> patterns) {
            super(patterns);
        }

        /**
         * returns 'true' if no pattern matches
         */
        @Override
        public boolean accept(String value) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).find()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * a 'Blacklist' is always a restriction
         */
        @Override
        public boolean isRestriction() {
            return true;
        }

        /**
         * Returns the string representation of the filter itself [-'filter']
         */
        @Override
        public void toString(StringBuilder builder) {
            builder.append("-'");
            super.toString(builder);
            builder.append("'");
        }
    }

    /**
     * An implementation to combine StringFilters to complex rules.
     */
    class FilterSet extends FilterBase {

        /**
         * the combination rule options:
         * - and:   each pattern in the set must accept a value
         * - or:    only one pattern in the set must accept a value
         * - first: the first appropriate filter determines the result
         * - last:  the last appropriate filter determines the result
         */
        public enum Rule {
            and, or, first, last
        }

        /** the selected combination rule for this filter set */
        protected final Rule rule;
        /** the set of combined filters collected in this set */
        protected final List<StringFilter> set;

        /** the cached value for the 'restriction' aspect */
        protected transient Boolean restriction;

        /**
         * Combines a set of filter instances by a combination rule
         *
         * @param rule    the combination rule
         * @param filters the set of combined filters
         */
        public FilterSet(Rule rule, StringFilter... filters) {
            this.rule = rule;
            this.set = new ArrayList<>();
            Collections.addAll(this.set, filters);
        }

        /**
         * Combines a set of filter instances by a combination rule
         *
         * @param rule    the combination rule
         * @param filters the set of combined filters
         */
        public FilterSet(Rule rule, List<StringFilter> filters) {
            this.rule = rule;
            this.set = filters;
        }

        /**
         * Returns the operator of the filter set.
         */
        public Rule getRule() {
            return rule;
        }

        /**
         * Returns the list of filter elements of the filter set.
         */
        public List<StringFilter> getSet() {
            return set;
        }

        /**
         * Accepts a value if the combination by the selected rule matches to the string.
         *
         * @param value the string value to check
         * @return 'true', if the value matches
         */
        @Override
        public boolean accept(String value) {
            switch (rule) {
                case or:
                    for (StringFilter filter : set) {
                        if (filter.accept(value)) {
                            return true;
                        }
                    }
                    return set.size() == 0;
                case and:
                    for (StringFilter filter : set) {
                        if (!filter.accept(value)) {
                            return false;
                        }
                    }
                    return set.size() > 0;
                case first:
                    for (StringFilter filter : set) {
                        if (filter.accept(value) && !filter.isRestriction()) {
                            return true;
                        }
                        if (!filter.accept(value) && filter.isRestriction()) {
                            return false;
                        }
                    }
                    return false;
                case last:
                    boolean result = false;
                    for (StringFilter filter : set) {
                        if (filter.accept(value) && !filter.isRestriction()) {
                            result = true;
                        }
                        if (!filter.accept(value) && filter.isRestriction()) {
                            result = false;
                        }
                    }
                    return result;
            }
            return isRestriction();
        }

        /**
         * it's difficult to determine a right value for such a set
         *
         * @return 'true', if one element in the set defines a restriction
         * and the combination rule is 'and' or 'first', otherwise 'false'
         */
        @Override
        public boolean isRestriction() {
            if (restriction == null) {
                switch (rule) {
                    case or:
                    case last:
                        restriction = true;
                        // return 'true' if each filter in the set is a 'restriction'
                        for (StringFilter filter : set) {
                            if (!filter.isRestriction()) {
                                restriction = false;
                                break;
                            }
                        }
                        break;
                    case and:
                    case first:
                        restriction = false;
                        // return 'true' if one filter in the set is a 'restriction'
                        for (StringFilter filter : set) {
                            if (filter.isRestriction()) {
                                restriction = true;
                                break;
                            }
                        }
                        break;
                }
            }
            return restriction;
        }

        /**
         * Returns the string representation of the filter itself ['rule'{'filter', ...}]
         */
        @SuppressWarnings("Duplicates")
        @Override
        public void toString(StringBuilder builder) {
            builder.append(rule.name());
            builder.append("{");
            for (int i = 0; i < set.size(); ) {
                set.get(i).toString(builder);
                if (++i < set.size()) {
                    builder.append(',');
                }
            }
            builder.append("}");
        }
    }
}
