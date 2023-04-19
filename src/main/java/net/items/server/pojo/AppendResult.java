package net.items.server.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Getter
@Setter
public class AppendResult {

    /**
     * 被请求方的任期号，用于领导人去更新自己
     */
    long term;

    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    boolean success;

    public AppendResult(long term) {
        this.term = term;
    }

    public AppendResult(boolean success) {
        this.success = success;
    }

    public AppendResult(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

    private AppendResult(Builder builder) {
        setTerm(builder.term);
        setSuccess(builder.success);
    }

    public static AppendResult fail() {
        return new AppendResult(false);
    }

    public static AppendResult ok() {
        return new AppendResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {

        private long term;
        private boolean success;

        private Builder() {
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder success(boolean val) {
            success = val;
            return this;
        }

        public AppendResult build() {
            return new AppendResult(this);
        }
    }

}
