/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.gorm.titan.plugin.support

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.titan.TitanGormEnhancer
import org.grails.datastore.gorm.titan.TitanGormInstanceApi
import org.grails.datastore.gorm.titan.TitanGormStaticApi
import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

/**
 * Methods configurer implementation for Neo4j.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class TitanMethodsConfigurer extends DynamicMethodsConfigurer {

    TitanMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() { "Titan" }

    @Override
    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        return new TitanGormStaticApi(cls, datastore, finders, transactionManager)
    }

    @Override
    protected GormInstanceApi createGormInstanceApi(Class cls) {
        def api = new TitanGormInstanceApi(cls, datastore)
        api.failOnError = failOnError
        api
    }

    @Override
    protected GormEnhancer createEnhancer() {
        def ge = new TitanGormEnhancer(datastore, transactionManager)
        ge.failOnError = failOnError
        ge
    }
}
