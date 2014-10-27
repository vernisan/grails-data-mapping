/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.gorm.titan.bean.factory

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.titan.TitanDatastore
import org.grails.datastore.gorm.titan.engine.CypherEngine
import org.neo4j.graphdb.GraphDatabaseService
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.context.ConfigurableApplicationContext

/**
 * Factory bean for constructing a {@link Neo4jDatastore} instance.
 *
 * @author Stefan Armbruster
 */
@CompileStatic
class TitanDatastoreFactoryBean implements FactoryBean<TitanDatastore>, ApplicationContextAware {

    MappingContext mappingContext
    Map<String,String> config = [:]
    ApplicationContext applicationContext
    CypherEngine cypherEngine

    TitanDatastore getObject() {

        TitanDatastore datastore = new TitanDatastore(mappingContext, applicationContext, cypherEngine)

        ((ConfigurableApplicationContext) applicationContext).addApplicationListener new DomainEventListener(datastore)
        ((ConfigurableApplicationContext) applicationContext).addApplicationListener new AutoTimestampEventListener(datastore)

        datastore.afterPropertiesSet()
        datastore
    }

    Class<?> getObjectType() { TitanDatastore }

    boolean isSingleton() { true }
}
