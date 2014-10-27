package org.grails.datastore.gorm.titan;

import org.grails.datastore.gorm.titan.GraphPersistentEntity;
import org.grails.datastore.gorm.titan.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefan on 15.02.14.
 */

class RelationshipPendingDelete extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingDelete.class);

    private String relType;
    private CypherEngine cypherEngine;
    private EntityAccess target;

    RelationshipPendingDelete(EntityAccess source, String relType, EntityAccess target, CypherEngine cypherEngine) {
        super(source.getPersistentEntity(), -1l, source.getEntity(), source);
        this.target = target;
        this.cypherEngine = cypherEngine;
        this.relType = relType;
    }

    @Override
    public void run() {
        String labelsFrom = ((GraphPersistentEntity)getEntity()).getLabelsAsString();
        String labelsTo = null;
        String cypher;

        List params =  new ArrayList(2);
        params.add(getEntityAccess().getIdentifier());
        if (target!=null) {
            params.add(target.getIdentifier());
            labelsTo = ((GraphPersistentEntity)target.getPersistentEntity()).getLabelsAsString();
            cypher = String.format("MATCH (from%s {__id__: {1}})-[r:%s]->(to%s {__id__: {2}}) DELETE r", labelsFrom, relType, labelsTo);
        } else {
            cypher = String.format("MATCH (from%s {__id__: {1}})-[r:%s]->() DELETE r", labelsFrom, relType);

        }
        cypherEngine.execute(cypher, params);
    }

}
