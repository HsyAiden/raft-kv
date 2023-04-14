package net.items.server.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Setter
@Getter
@Builder
public class VoteResult {

    /**
     * 当前任期号
     */
    private long term;

    /**
     * 是否统一投票
     */
    private boolean voteGranted;

    public VoteResult(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    private VoteResult(Builder builder) {
        setTerm(builder.term);
        setVoteGranted(builder.voteGranted);
    }

    public static VoteResult fail() {
        return new VoteResult(false);
    }

    public static VoteResult ok() {
        return new VoteResult(true);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        private long term;
        private boolean voteGranted;

        private Builder() {
        }

        public Builder term(long term) {
            this.term = term;
            return this;
        }

        public Builder voteGranted(boolean voteGranted) {
            this.voteGranted = voteGranted;
            return this;
        }

        public VoteResult build() {
            return new VoteResult(this);
        }
    }
}
