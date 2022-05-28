/*
 * Stalemate Game
 * Copyright (C) 2022 Weltspear
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.stalemate.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class StFormatter extends Formatter {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);

        // colors
        if (Level.FINE.equals(record.getLevel())) {
            builder.append(ANSI_GREEN);
        }
        else if (Level.WARNING.equals(record.getLevel())) {
            builder.append(ANSI_YELLOW);
        }
        else if (Level.SEVERE.equals(record.getLevel())) {
            builder.append(ANSI_RED);
        }
        else if (Level.INFO.equals(record.getLevel())) {
            builder.append(ANSI_BLUE);
        }

        builder.append("[");
        builder.append(record.getLoggerName());
        builder.append("] ");
        builder.append("[");
        builder.append(df.format(new Date(record.getMillis()))).append("] ");
        builder.append("[").append(record.getLevel()).append("]: ");
        builder.append(formatMessage(record));
        builder.append("\n");
        return builder.toString();
    }

    public String getHead(Handler h) {
        return super.getHead(h);
    }

    public String getTail(Handler h) {
        return super.getTail(h);
    }
}
