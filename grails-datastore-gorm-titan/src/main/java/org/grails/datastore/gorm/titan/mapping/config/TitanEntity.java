package org.grails.datastore.gorm.titan.mapping.config;

import org.grails.datastore.mapping.config.Entity;

/**
 * Created by stefan on 10.04.14.
 * @author Stefan Armbruster
 */
public class TitanEntity extends Entity {

    private Object labels;

    public Object getLabels() {
        return labels;
    }

    public void setLabels(Object labels) {
        this.labels = labels;
    }
}
