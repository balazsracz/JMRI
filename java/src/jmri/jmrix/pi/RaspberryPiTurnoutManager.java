package jmri.jmrix.pi;

import jmri.Turnout;

/**
 * Implement Pi turnout manager.
 * <p>
 * System names are "PTnnn", where P is the user configurable system prefix,
 * nnn is the turnout number without padding.
 *
 * @author   Paul Bender Copyright (C) 2015
 */
public class RaspberryPiTurnoutManager extends jmri.managers.AbstractTurnoutManager {

    private String prefix = null;
    private RaspberryPiSystemConnectionMemo _memo;

    // ctor with a full memo
    public RaspberryPiTurnoutManager(RaspberryPiSystemConnectionMemo memo) {
        super();
        _memo = memo;
        this.prefix = memo.getSystemPrefix().toUpperCase();
    }

    // ctor has to register for RaspberryPi events
    public RaspberryPiTurnoutManager(String prefix) {
        super();
        this.prefix = prefix.toUpperCase();
    }

    /**
     * Provides access to the system prefix string.
     * This was previously called the "System letter".
     */
    @Override
    public String getSystemPrefix() {
        return prefix;
    }

    @Override
    public Turnout createNewTurnout(String systemName, String userName) {
        Turnout t = new RaspberryPiTurnout(systemName, userName);
        return t;
    }

    /** {@inheritDoc} */
    @Override
    public int getOutputInterval(String systemName) {
        if (_memo == null) {
            return 250;
        } else {
            return _memo.getOutputInterval();
        }
    }

}
