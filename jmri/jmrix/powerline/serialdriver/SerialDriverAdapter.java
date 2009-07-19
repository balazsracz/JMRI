// SerialDriverAdapter.java

package jmri.jmrix.powerline.serialdriver;

import jmri.jmrix.powerline.SerialPortController;
import jmri.jmrix.powerline.SerialSensorManager;
import jmri.jmrix.powerline.SerialTrafficController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;

/**
 * Provide access to Oak Tree via a serial comm port.
 * Normally controlled by the oaktree.serialdriver.SerialDriverFrame class.
 * @author			Bob Jacobsen   Copyright (C) 2006, 2007, 2008
 * @version			$Revision: 1.10 $
 */
public class SerialDriverAdapter extends SerialPortController implements jmri.jmrix.SerialPortAdapter {

    SerialPort activeSerialPort = null;

    public String openPort(String portName, String appName)  {
        try {
            // get and open the primary port
            CommPortIdentifier portID = CommPortIdentifier.getPortIdentifier(portName);
            try {
                activeSerialPort = (SerialPort) portID.open(appName, 2000);  // name of program, msec to wait
            }
            catch (PortInUseException p) {
                return handlePortBusy(p, portName, log);
            }
            // try to set it for serial
            try {
                setSerialPort();
            } catch (javax.comm.UnsupportedCommOperationException e) {
                log.error("Cannot set serial parameters on port "+portName+": "+e.getMessage());
                return "Cannot set serial parameters on port "+portName+": "+e.getMessage();
            }


            // set framing (end) character
            try {
                log.debug("Serial framing was observed as: "+activeSerialPort.isReceiveFramingEnabled()
                      +" "+activeSerialPort.getReceiveFramingByte());
            } catch (Exception ef) {
                log.info("failed to set serial framing: "+ef);
            }

            // set timeout; framing should work before this anyway
            try {
                activeSerialPort.enableReceiveTimeout(10);
                log.debug("Serial timeout was observed as: "+activeSerialPort.getReceiveTimeout()
                      +" "+activeSerialPort.isReceiveTimeoutEnabled());
            } catch (Exception et) {
                log.info("failed to set serial timeout: "+et);
            }
            
            // get and save stream
            serialStream = activeSerialPort.getInputStream();

            // purge contents, if any
            int count = serialStream.available();
            log.debug("input stream shows "+count+" bytes available");
            while ( count > 0) {
                serialStream.skip(count);
                count = serialStream.available();
            }

            // report status?
            if (log.isInfoEnabled()) {
                // report now
                log.info(portName+" port opened at "
                         +activeSerialPort.getBaudRate()+" baud with"
                         +" DTR: "+activeSerialPort.isDTR()
                         +" RTS: "+activeSerialPort.isRTS()
                         +" DSR: "+activeSerialPort.isDSR()
                         +" CTS: "+activeSerialPort.isCTS()
                         +"  CD: "+activeSerialPort.isCD()
                         );
            }
            if (log.isDebugEnabled()) {
                // report additional status
                log.debug(" port flow control shows "+
                          (activeSerialPort.getFlowControlMode()==SerialPort.FLOWCONTROL_RTSCTS_OUT?"hardware flow control":"no flow control"));
            }
            if (log.isDebugEnabled()) {
                // arrange to notify later
                activeSerialPort.addEventListener(new SerialPortEventListener(){
                        public void serialEvent(SerialPortEvent e) {
                            int type = e.getEventType();
                            switch (type) {
                            case SerialPortEvent.DATA_AVAILABLE:
                                log.info("SerialEvent: DATA_AVAILABLE is "+e.getNewValue());
                                return;
                            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                                log.info("SerialEvent: OUTPUT_BUFFER_EMPTY is "+e.getNewValue());
                                return;
                            case SerialPortEvent.CTS:
                                log.info("SerialEvent: CTS is "+e.getNewValue());
                                return;
                            case SerialPortEvent.DSR:
                                log.info("SerialEvent: DSR is "+e.getNewValue());
                                return;
                            case SerialPortEvent.RI:
                                log.info("SerialEvent: RI is "+e.getNewValue());
                                return;
                            case SerialPortEvent.CD:
                                log.info("SerialEvent: CD is "+e.getNewValue());
                                return;
                            case SerialPortEvent.OE:
                                log.info("SerialEvent: OE (overrun error) is "+e.getNewValue());
                                return;
                            case SerialPortEvent.PE:
                                log.info("SerialEvent: PE (parity error) is "+e.getNewValue());
                                return;
                            case SerialPortEvent.FE:
                                log.info("SerialEvent: FE (framing error) is "+e.getNewValue());
                                return;
                            case SerialPortEvent.BI:
                                log.info("SerialEvent: BI (break interrupt) is "+e.getNewValue());
                                return;
                            default:
                                log.info("SerialEvent of unknown type: "+type+" value: "+e.getNewValue());
                                return;
                            }
                        }
                    }
                                                  );
                try { activeSerialPort.notifyOnFramingError(true); }
                catch (Exception e) { log.debug("Could not notifyOnFramingError: "+e); }

                try { activeSerialPort.notifyOnBreakInterrupt(true); }
                catch (Exception e) { log.debug("Could not notifyOnBreakInterrupt: "+e); }

                try { activeSerialPort.notifyOnParityError(true); }
                catch (Exception e) { log.debug("Could not notifyOnParityError: "+e); }

                try { activeSerialPort.notifyOnOverrunError(true); }
                catch (Exception e) { log.debug("Could not notifyOnOverrunError: "+e); }

            }

            opened = true;

        } catch (javax.comm.NoSuchPortException p) {
            return handlePortNotFound(p, portName, log);
        } catch (Exception ex) {
            log.error("Unexpected exception while opening port "+portName+" trace follows: "+ex);
            ex.printStackTrace();
            return "Unexpected error while opening port "+portName+": "+ex;
        }

        return null; // normal operation
    }

    /**
     * Can the port accept additional characters? Yes, always
     */
    public boolean okToSend() {
        return true;
    }

    /**
     * set up all of the other objects to operate
     * connected to this port
     */
    public void configure() {
        // set up the system connection first
        String opt1 = getCurrentOption1Setting();
        if (opt1.equals("CM11")) {
            // create a CM11 port controller
            SerialTrafficController.checkInstance(new jmri.jmrix.powerline.cm11.SpecificTrafficController());
        } else if (opt1.equals("CP290")) {
            // create a CP290 port controller
            SerialTrafficController.checkInstance(new jmri.jmrix.powerline.cp290.SpecificTrafficController());
        } else {
            // no connection at all - warn
            log.warn("protocol option "+opt1+" defaults to CM11");
            // create a CM11 port controller
            SerialTrafficController.checkInstance(new jmri.jmrix.powerline.cm11.SpecificTrafficController());
        }    

        // connect to the traffic controller
        SerialTrafficController.instance().connectPort(this);

        SerialSensorManager s;
        jmri.InstanceManager.setSensorManager(s = new jmri.jmrix.powerline.SerialSensorManager());
        SerialTrafficController.instance().setSensorManager(s);

        jmri.InstanceManager.setTurnoutManager(new jmri.jmrix.powerline.SerialTurnoutManager());

        // define the appropriate LightManager
        if (opt1.equals("CM11")) {
            jmri.InstanceManager.setLightManager(new jmri.jmrix.powerline.cm11.SpecificLightManager());
        } else if (opt1.equals("CP290")) {
            jmri.InstanceManager.setLightManager(new jmri.jmrix.powerline.cp290.SpecificLightManager());
        }   

        // declare up
        jmri.jmrix.powerline.ActiveFlag.setActive();
    }

    // base class methods for the SerialPortController interface
    public DataInputStream getInputStream() {
        if (!opened) {
            log.error("getInputStream called before load(), stream not available");
            return null;
        }
        return new DataInputStream(serialStream);
    }

    public DataOutputStream getOutputStream() {
        if (!opened) log.error("getOutputStream called before load(), stream not available");
        try {
            return new DataOutputStream(activeSerialPort.getOutputStream());
        }
     	catch (java.io.IOException e) {
            log.error("getOutputStream exception: "+e.getMessage());
     	}
     	return null;
    }

    public boolean status() {return opened;}

    /**
     * Local method to do specific port configuration
     */
    protected void setSerialPort() throws javax.comm.UnsupportedCommOperationException {
        // find the baud rate value, configure comm options
        int baud = 4800;  // default, but also defaulted in the initial value of selectedSpeed
        
        // check for specific port type
        String opt1 = getCurrentOption1Setting();
        if (opt1.equals("CM11")) {
            // leave as 4800 baud
        } else if (opt1.equals("CP290")) { 
            // set to 600 baud
            baud = 600;
        }
        
        activeSerialPort.setSerialPortParams(baud, SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        // set RTS high, DTR high - done early, so flow control can be configured after
        activeSerialPort.setRTS(true);		// not connected in some serial ports and adapters
        activeSerialPort.setDTR(true);		// pin 1 in DIN8; on main connector, this is DTR

        // find and configure flow control
        int flow = SerialPort.FLOWCONTROL_NONE; // default
        activeSerialPort.setFlowControlMode(flow);
    }

    /**
     * Get an array of valid baud rates.
     */
    public String[] validBaudRates() {
        return validSpeeds;
    }

    /**
     * Set the baud rate.
     */
    public void configureBaudRate(String rate) {
        log.debug("configureBaudRate: "+rate);
        selectedSpeed = rate;
        super.configureBaudRate(rate);
    }

    String[] stdOption1Values = new String[]{"CM11", "CP290"}; 

    /**
     * Option 1 is not used for anything
     */
    public String[] validOption1() { return stdOption1Values; }

    /**
     * Option 1 not used, so return a null string.
     */
    public String option1Name() { return "Adapter"; }

    
    protected String [] validSpeeds = new String[]{"(automatic)"};
    protected int [] validSpeedValues = new int[]{4800};
    protected String selectedSpeed=validSpeeds[0];

    /**
     * Get an array of valid values for "option 2"; used to display valid options.
     * May not be null, but may have zero entries
     */
    public String[] validOption2() { return new String[]{""}; }

    /**
     * Get a String that says what Option 2 represents
     * May be an empty string, but will not be null
     */
    public String option2Name() { return ""; }

    // private control members
    private boolean opened = false;
    InputStream serialStream = null;

    static public SerialDriverAdapter instance() {
        if (mInstance == null) mInstance = new SerialDriverAdapter();
        return mInstance;
    }
    static SerialDriverAdapter mInstance = null;

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SerialDriverAdapter.class.getName());

}
