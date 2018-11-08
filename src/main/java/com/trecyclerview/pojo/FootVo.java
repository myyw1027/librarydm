package com.trecyclerview.pojo;

/**
 * @author：tqzhang on 18/7/13 17:58
 */
public class FootVo {
    public String desc;

    public int state;

    public FootVo(int state) {
        this.state = state;
    }

    public FootVo(int state, String desc) {
        this.state = state;
        this.desc = desc;
    }
}
