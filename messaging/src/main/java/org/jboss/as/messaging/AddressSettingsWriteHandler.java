/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.messaging;


import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class AddressSettingsWriteHandler implements OperationStepHandler {

    static final OperationStepHandler INSTANCE = new AddressSettingsWriteHandler();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final String attribute = operation.require(ModelDescriptionConstants.NAME).asString();
        final AttributeDefinition def = getAttributeDefinition(attribute);
        if(def == null) {
            context.getFailureDescription().set(new ModelNode().set(String.format("no such attribute (%s) ", attribute)));
        }
        def.getValidator().validateParameter(ModelDescriptionConstants.VALUE, operation);
        resource.getModel().get(attribute).set(operation.get(ModelDescriptionConstants.VALUE));
        if(context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final HornetQServer server = AddressSettingAdd.getServer(context);
                    if(server != null) {
                        final ModelNode model = resource.getModel();
                        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                        final AddressSettings settings = AddressSettingAdd.createSettings(model);
                        server.getAddressSettingsRepository().addMatch(address.getLastElement().getValue(), settings);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    static final AttributeDefinition getAttributeDefinition(final String attributeName) {
        for(final AttributeDefinition def : AddressSettingAdd.ATTRIBUTES) {
            if(def.getName().equals(attributeName)) {
                return def;
            }
        }
        return null;
    }

}
