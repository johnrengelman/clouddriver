/*
 * Copyright 2017 Netflix, Inc.
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

package com.netflix.spinnaker.clouddriver.kubernetes.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.kubernetes.cache.Keys
import com.netflix.spinnaker.clouddriver.kubernetes.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE

@Slf4j
class KubernetesNamespaceCachingAgent extends KubernetesCachingAgent {


  static final Set<AgentDataType> types = Collections.unmodifiableSet([
    AUTHORITATIVE.forType(Keys.Namespace.NAMESPACES.ns),
  ] as Set)

  KubernetesNamespaceCachingAgent(String accountName, ObjectMapper objectMapper, KubernetesCredentials credentials, int agentIndex, int agentCount) {
    super(accountName, objectMapper, credentials, agentIndex, agentCount)
  }

  @Override
  String getSimpleName() {
    return KubernetesNamespaceCachingAgent.simpleName
  }

  @Override
  Collection<AgentDataType> getProvidedDataTypes() {
    return types
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Loading namespaces in $agentType")
    buildCacheResult()
  }

  private CacheResult buildCacheResult() {
    log.info("Describing items in ${agentType}")

    Map<String, MutableCacheData> cachedNamespaces = MutableCacheData.mutableCacheMap()

    def namespaces = lookupNamespaces()
    def key = Keys.getNamespaceKey(accountName)
    cachedNamespaces[key].with {
      attributes.account = accountName
      attributes.namespaces = namespaces
    }

    log.info("Caching ${namespaces.size()} namespaces in ${agentType}")
    return new DefaultCacheResult([
      (Keys.Namespace.NAMESPACES.ns): cachedNamespaces.values(),
    ], [:])
  }

  private List<String> lookupNamespaces() {
    if (credentials.configuredNamespaces) {
      // If namespaces are provided, used them
      return credentials.configuredNamespaces
    } else {
      List<String> addedNamespaces = credentials.apiAdaptor.getNamespacesByName()
      addedNamespaces.removeAll(credentials.omitNamespaces)

      List<String> resultNamespaces = new ArrayList<>(addedNamespaces)

      // Find the namespaces that were added, and add docker secrets to them. No need to track deleted
      // namespaces since they delete their secrets automatically.
      addedNamespaces.removeAll(credentials.oldNamespaces)
      credentials.reconfigureRegistries(addedNamespaces, resultNamespaces)
      credentials.oldNamespaces = resultNamespaces

      return resultNamespaces
    }
  }
}
