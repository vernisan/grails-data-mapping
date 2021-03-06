/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.orm.HibernateCriteriaBuilder
import grails.orm.PagedResultList
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.orm.hibernate.query.GrailsHibernateQueryUtils
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.query.api.BuildableCriteria as GrailsCriteria
import org.hibernate.Criteria
import org.hibernate.LockMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.core.convert.ConversionService
import org.springframework.orm.hibernate3.HibernateCallback
import org.springframework.orm.hibernate3.HibernateTemplate
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * The implementation of the GORM static method contract for Hibernate
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class HibernateGormStaticApi<D> extends AbstractHibernateGormStaticApi<D> {
    protected GrailsHibernateTemplate hibernateTemplate
    protected SessionFactory sessionFactory
    protected ConversionService conversionService
    protected Class identityType
    protected ClassLoader classLoader
    protected GrailsApplication grailsApplication
    protected boolean cacheQueriesByDefault = false
    private HibernateGormInstanceApi<D> instanceApi

    HibernateGormStaticApi(Class<D> persistentClass, HibernateDatastore datastore, List<FinderMethod> finders,
                ClassLoader classLoader, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager, new GrailsHibernateTemplate(datastore.sessionFactory))
        this.classLoader = classLoader
        sessionFactory = datastore.getSessionFactory()
        conversionService = datastore.mappingContext.conversionService

        identityType = persistentEntity.identity?.type

        grailsApplication = datastore.getGrailsApplication()
        if (grailsApplication != null) {
            GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            identityType = domainClass.identifier?.type

            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory)
            hibernateTemplate.setCacheQueries(cacheQueriesByDefault)
        } else {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory)
        }

        instanceApi = new HibernateGormInstanceApi<>(persistentClass, datastore, classLoader)
    }

    @Override
    List<D> list(Map params = Collections.emptyMap()) {
        hibernateTemplate.execute { Session session ->
            Criteria c = session.createCriteria(persistentEntity.javaClass)
            hibernateTemplate.applySettings c

            params = params ? new HashMap(params) : Collections.emptyMap()
            setResultTransformer(c)
            if(params.containsKey(DynamicFinder.ARGUMENT_MAX)) {

                c.setMaxResults(Integer.MAX_VALUE)
                GrailsHibernateQueryUtils.populateArgumentsForCriteria(persistentEntity, c, params, datastore.mappingContext.conversionService, true)
                return new PagedResultList(hibernateTemplate, c)
            }
            else {
                GrailsHibernateQueryUtils.populateArgumentsForCriteria(persistentEntity, c, params, datastore.mappingContext.conversionService, true)
                return c.list()
            }
        }
    }

    @CompileDynamic
    protected void setResultTransformer(Criteria c) {
        c.resultTransformer = Criteria.DISTINCT_ROOT_ENTITY
    }

    @Override
    GrailsCriteria createCriteria() {
        def builder = new HibernateCriteriaBuilder(persistentClass, sessionFactory)
        builder.grailsApplication = grailsApplication
        builder.conversionService = conversionService
        builder
    }

    @Override
    D lock(Serializable id) {
        id = convertIdentifier(id)
        (D)hibernateTemplate.get((Class)persistentClass, id, LockMode.UPGRADE)
    }

    @Override
    D merge( D o ) {
        instanceApi.merge(o)
    }

    @Override
    Object withSession(Closure callable) {
        HibernateTemplate template = new GrailsHibernateTemplate(sessionFactory)
        template.setExposeNativeSession(false)
        template.execute({ session ->
            callable(session)
        } as HibernateCallback)
    }

    @Override
    Integer executeUpdate(String query, Map params, Map args) {
        def template = hibernateTemplate
        return (Integer) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)
            SessionFactoryUtils.applyTransactionTimeout q, template.sessionFactory


            populateQueryArguments(q, params)
            populateQueryArguments(q, args)
            populateQueryWithNamedArguments(q, params)

            return q.executeUpdate()
        }
    }

    @Override
    Integer executeUpdate(String query, Collection params, Map args) {
        def template = hibernateTemplate
        return (Integer) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)
            SessionFactoryUtils.applyTransactionTimeout q, template.sessionFactory

            params.eachWithIndex { val, int i ->
                if (val instanceof CharSequence) {
                    q.setParameter i, val.toString()
                }
                else {
                    q.setParameter i, val
                }
            }
            populateQueryArguments(q, args)
            return q.executeUpdate()
        }
    }

    @Override
    def withNewSession(Closure callable) {
        HibernateTemplate template  = new GrailsHibernateTemplate(sessionFactory, grailsApplication)
        template.setExposeNativeSession(false)
        SessionHolder sessionHolder = (SessionHolder)TransactionSynchronizationManager.getResource(sessionFactory)
        Session previousSession = sessionHolder?.session
        Session newSession
        boolean newBind = false
        try {
            template.allowCreate = true
            newSession = sessionFactory.openSession()
            if (sessionHolder == null) {
                sessionHolder = new SessionHolder(newSession)
                TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder)
                newBind = true
            }
            else {
                sessionHolder.addSession(newSession)
            }
            template.execute({ Session session ->
                return callable(session)
            } as HibernateCallback)
        }
        finally {
            if (newSession) {
                SessionFactoryUtils.closeSession(newSession)
                sessionHolder?.removeSession(newSession)
            }
            if (newBind) {
                TransactionSynchronizationManager.unbindResource(sessionFactory)
            }
            if (previousSession) {
                sessionHolder?.addSession(previousSession)
            }
        }
    }

}
