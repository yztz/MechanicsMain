package me.deecaad.core.utils;

import java.util.logging.Logger;

public class Debugger {

    // Which logger to use to log information,
    // no logical reason to be able to change this
    private final Logger logger;
    private int level;

    public Debugger(Logger logger, int level) {
        this.logger = logger;
        this.level = level;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the current logging level
     *
     * @return Integer logging level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the logger level, should be called after
     * reloading the plugin that instantiated this class
     *
     * @param level The integer level to log at
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Determines if the given level can be logged
     *
     * @param level Level to test for
     * @return true if can be logged
     */
    public boolean canLog(LogLevel level) {
        return level.shouldPrint(this.level);
    }

    /**
     * Logs all given messaged at the given
     * <code>LogLevel</code>. Each message is
     * logged on a new line.
     *
     * @param level The level to log at
     * @param msg The messages
     */
    public void log(LogLevel level, String...msg) {
        if (!canLog(level)) return;

        for (String str : msg) {
            logger.log(level.getParallel(), str);
        }
    }

    /**
     * Logs the given error and gives the user the
     * given message.
     *
     * @param level Level to log at
     * @param msg Message to send
     * @param error Error
     */
    public void log(LogLevel level, String msg, Throwable error) {
        if (!canLog(level)) return;

        logger.log(level.getParallel(), msg, error);
    }

    /**
     * Shorthand for asserting true with an <code>ERROR</code>
     * <code>LoggingLevel</code>.
     *
     * @param bool What to assert
     * @param messages Messages to log if assertion failed
     */
    public void validate(boolean bool, String...messages) {
        if (!bool) {
            log(LogLevel.ERROR, messages);
        }
    }

    /**
     * Easy way to assert variables cleanly. If <code>bool</code>
     * is true, than there is no error, the method exits. If it
     * is false, than there is an error, and the given messages
     * should be logged at the given level so the user can be
     * aware of possible errors.
     *
     * @param level Level to log at
     * @param bool What to assert
     * @param messages Messages to log if assertion failed
     */
    public void validate(LogLevel level, boolean bool, String...messages) {
        if (!bool) {
            log(level, messages);
        }
    }
}
