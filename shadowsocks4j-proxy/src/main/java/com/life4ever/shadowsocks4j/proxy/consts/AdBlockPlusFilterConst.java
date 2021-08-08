package com.life4ever.shadowsocks4j.proxy.consts;

public class AdBlockPlusFilterConst {

    public static final String DOMAIN_NAME_PRECISE_MATCHER = "@@|";

    public static final String DOMAIN_NAME_FUZZY_MATCHER = "@@||";

    public static final String PRECISE_MATCHER_REGEX_EXPRESSION = "^\\@\\@\\|\\w+";

    public static final String FUZZY_MATCHER_REGEX_EXPRESSION = "^\\@\\@\\|\\|\\w+";

    public static final String WHITE_LIST_START_FLAG = "Whitelist Start";

    private AdBlockPlusFilterConst() {
    }

}