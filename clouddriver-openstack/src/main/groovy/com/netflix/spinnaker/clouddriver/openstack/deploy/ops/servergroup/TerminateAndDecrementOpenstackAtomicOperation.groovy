/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.servergroup

import com.netflix.spinnaker.clouddriver.openstack.deploy.description.servergroup.ServerGroupParameters
import com.netflix.spinnaker.clouddriver.openstack.deploy.description.servergroup.TerminateAndDecrementOpenstackDescription
import com.netflix.spinnaker.clouddriver.openstack.deploy.ops.instance.TerminateOpenstackInstancesAtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import org.openstack4j.model.heat.Stack

/**
 * This operation first marks the specified instances as unhealthy and then
 * executing an stack update with the desired size decremented by the number
 * of instances. The subsequent stack update will terminate the unhealthy instances
 * in order to achieve the new desired size
 */
class TerminateAndDecrementOpenstackAtomicOperation extends TerminateOpenstackInstancesAtomicOperation {

  final String phaseName = "TERMINATE_AND_DEC_INSTANCES"

  final String operation = AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT

  TerminateAndDecrementOpenstackAtomicOperation(TerminateAndDecrementOpenstackDescription description) {
    super(description)
  }

  @Override
  ServerGroupParameters buildServerGroupParameters(Stack stack) {
    TerminateAndDecrementOpenstackDescription opDescription = (TerminateAndDecrementOpenstackDescription) description
    ServerGroupParameters params = super.buildServerGroupParameters(stack)
    params.desiredSize = params.desiredSize - opDescription.instanceIds.size()
    params
  }
}
