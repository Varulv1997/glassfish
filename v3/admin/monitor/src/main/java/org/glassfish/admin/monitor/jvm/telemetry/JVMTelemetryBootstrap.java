/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admin.monitor.jvm.telemetry;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.monitoring.TelemetryProvider;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import org.glassfish.flashlight.datatree.TreeNode;
import org.glassfish.flashlight.datatree.factory.TreeNodeFactory;
import org.jvnet.hk2.component.PostConstruct;

/**
 *
 * @author PRASHANTH ABBAGANI
 */
@Service(name="jvm")
@Scoped(Singleton.class)
public class JVMTelemetryBootstrap implements TelemetryProvider, PostConstruct {

    @Inject
    Logger logger;
    @Inject
    private Domain domain;
    @Inject
    private MonitoringRuntimeDataRegistry mrdr;

    private boolean jvmMonitoringEnabled = false;
    private JVMStatsTelemetry jvmTM = null;
    private TreeNode serverNode;

    public JVMTelemetryBootstrap() {
    }

    public void postConstruct(){
        // to set log level, uncomment the following 
        // remember to comment it before checkin
        // remove this once we find a proper solution
        Level dbgLevel = Level.FINEST;
        Level defaultLevel = logger.getLevel();
        if ((defaultLevel == null) || (dbgLevel.intValue() < defaultLevel.intValue())) {
            //logger.setLevel(dbgLevel);
        }
        logger.finest("[Monitor]In the JVMTelemetry bootstrap ************");

        //Build the top level monitoring tree
        buildTopLevelMonitoringTree();        
    }
    
    public void onLevelChange(String newLevel) {
        boolean newLevelEnabledValue = getEnabledValue(newLevel);
        logger.finest("[Monitor]In the Level Change = " + newLevel + "  ************");
        if (jvmMonitoringEnabled != newLevelEnabledValue) {
            jvmMonitoringEnabled = newLevelEnabledValue;
        } else {
            // Might have changed from 'LOW' to 'HIGH' or vice-versa. Ignore.
            return;
        }
        //check if the monitoring level for web-container is 'on' and 
        // if Web Container is loaded, then register the ProveProviderListener
        if (jvmMonitoringEnabled) { 
            // enable flag turned from 'OFF' to 'ON'
              // (1)Could be that the telemetry object is not built
              // (2)Could be that the telemetry object is there but is disabled 
              //    explicitly by user. Now we need to enable them
            if (jvmTM != null)
                enableJVMMonitoring(true);
            else
                buildJVMTelemetry();
        } else { 
            //enable flag turned from 'ON' to 'OFF', so disable telemetry
            enableJVMMonitoring(false);
        }
    }

    private boolean getEnabledValue(String enabledStr) {
        if ("OFF".equals(enabledStr)) {
            return false;
        }
        return true;
    }

    //builds the top level tree
    private void buildTopLevelMonitoringTree() {
        //check if serverNode exists
        if (serverNode != null)
            return;
        if (mrdr.get("server") != null) {
            serverNode = mrdr.get("server");
            return;
        }
        // server
        Server srvr = null;
        List<Server> ls = domain.getServers().getServer();
        for (Server sr : ls) {
            if ("server".equals(sr.getName())) {
                srvr = sr;
                break;
            }
        }
        serverNode = TreeNodeFactory.createTreeNode("server", null, "server");
        mrdr.add("server", serverNode);
    }

    private void buildJVMTelemetry() {
        if (jvmTM == null) {
            jvmTM = new JVMStatsTelemetry(serverNode, logger);
        }
    }

    private void enableJVMMonitoring(boolean isEnabled) {
        //Enable/Disable jvm telemetry
        if (jvmTM != null)
            jvmTM.enableMonitoring(isEnabled);
    }
}
