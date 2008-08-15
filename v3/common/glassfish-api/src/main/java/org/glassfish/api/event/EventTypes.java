/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.api.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Extensible list of event types.
 * EventTypes are created through the create method and not directly.
 * 
 * Events can be compared using == or equals although == is recommended.
 * 
 * @author dochez
 */
public final class EventTypes {
    
    private final static Map<String, EventTypes> EVENTS=new HashMap<String, EventTypes>();

    
    // stock events.
    public static final String SERVER_READY_NAME = "server_ready";
    public static final String SERVER_SHUTDOWN_NAME = "server_shutdown";
    public static final EventTypes SERVER_READY = create(SERVER_READY_NAME);
    public static final EventTypes SERVER_SHUTDOWN = create(SERVER_SHUTDOWN_NAME);

    
    public static EventTypes create(String name) {
        synchronized(EVENTS) {
            if (!EVENTS.containsKey(name)) {
                EVENTS.put(name, new EventTypes(name));                
            }
        }
        return EVENTS.get(name);
    }    
    
    private final String name;

    private EventTypes(String name) {
        this.name = name;
    }
    
    public String type() {
        return name;
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * Considers only {@link #name} for equality.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (null == o) return false;
        if (getClass() != o.getClass()) return false;

        return name.equals(((EventTypes) o).name);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Returns {@link #name} as the hash code.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }      
    
}
