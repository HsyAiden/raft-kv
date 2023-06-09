package net.items.server.constant;

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
@Builder
public class Command {
    /**
     * 操作类型
     */
    CommandType type;

    String key;

    String value;
}
