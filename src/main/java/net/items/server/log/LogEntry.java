package net.items.server.log;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.items.server.constant.Command;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Setter
@Getter
@Builder
public class LogEntry {

    private Long index;

    private long term;

    private Command command;

    private String requestId;
}
