/************************************************************\
 *       Copyright (C) 2001 Appgen Personal Software        *
\************************************************************/

package com.moneydance.modules.features.mynetworth;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

import java.io.*;
import java.awt.*;

/** Pluggable module used to give users access to a Account List
    interface to Moneydance.
*/

public class Main
  extends FeatureModule
{
  private AccountBalancesWindow accountBalancesWindow = null;

  public void init() {
    // the first thing we will do is register this module to be invoked
    // via the application toolbar
    FeatureModuleContext context = getContext();
    try {
      context.registerFeature(this, "showconsole",
        getIcon(""),
        getName());
    }
    catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

  public void cleanup() {
    closeConsole();
  }
  
  private Image getIcon(String action) {
    try {
      ClassLoader cl = getClass().getClassLoader();
      java.io.InputStream in = 
        cl.getResourceAsStream("/com/moneydance/modules/features/mynetworth/icon.gif");
      if (in != null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
        byte buf[] = new byte[256];
        int n = 0;
        while((n=in.read(buf, 0, buf.length))>=0)
          bout.write(buf, 0, n);
        return Toolkit.getDefaultToolkit().createImage(bout.toByteArray());
      }
    } catch (Throwable e) { }
    return null;
  }
  
  /** Process an invokation of this module with the given URI */
  public void invoke(String uri) {
    String command = uri;
    String parameters = "";
    int theIdx = uri.indexOf('?');
    if(theIdx>=0) {
      command = uri.substring(0, theIdx);
      parameters = uri.substring(theIdx+1);
    }
    else {
      theIdx = uri.indexOf(':');
      if(theIdx>=0) {
        command = uri.substring(0, theIdx);
      }
    }

    if(command.equals("showconsole")) {
        showConsole();
    }    
  }

  public String getName() {
    return "Balances By Dates";

  }

   private synchronized void showConsole() {
        if(accountBalancesWindow ==null) {
            accountBalancesWindow = new AccountBalancesWindow(this);
            accountBalancesWindow.setVisible(true);
        }
        else {
            accountBalancesWindow.setVisible(true);
            accountBalancesWindow.toFront();
            accountBalancesWindow.requestFocus();
        }
    }

   FeatureModuleContext getUnprotectedContext() {
        return getContext();
  }

  synchronized void closeConsole() {
        if(accountBalancesWindow !=null) {
            accountBalancesWindow.goAway();
            accountBalancesWindow = null;
            System.gc();
        }
    }
}


