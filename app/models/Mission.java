/*
 * Copyright(c) 2010 Les Zindeps.
 *
 * The code source of this project is distributed
 * under the Affero GPL GNU AFFERO GENERAL PUBLIC LICENSE
 * Version 3, 19 November 2007
 *
 * This file is part of project LesZindeps. The source code is
 * hosted on GitHub. The initial project was launched by
 * Nicolas Martignole.
 *
 * LesZindeps is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LesZindeps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * Please see COPYING.AGPL.txt for the full text license
 * or online http://www.gnu.org/licenses/agpl.html
 */

package models;

import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Correspond aux anciennes missions réalisées par les Zindeps
 */
@Entity
public class Mission extends Model {
    public static final int PERCENTAGE_SCALE = 2;
    @ManyToOne
    public Zindep zindep;

    @Required
    public String title;

    @Required
    public String role;

    @Temporal(TemporalType.DATE)
    @Required
    public Date initialDate;

    public long length;

    @Temporal(TemporalType.DATE)
    @Required
    public Date endDate;

    @Required
    @Min(0)
    public Long clientPrice;

    @Min(0)
    public Long intermediaryPrice;

    @Required
    public String customer;

    public String intermediary;

    @Lob
    @MaxSize(10000)
    @Type(type = "org.hibernate.type.TextType")
    public String comment;

    @Required
    public String location;

    public String country;

    @Required(message = "Le champ expérience est obligatoire, merci d'indiquer un nombre d'années")
    public Long exp;


    /**
     * Override save method to compute automatically the mission length in days
     *
     * @return the mission saved
     */
    @Override
    public Mission save() {
        length = endDate.getTime() - initialDate.getTime();
        length = length / (3600 * 24 * 1000);
        return super.save();
    }


    /**
     * Retourne la liste des missions déjà effectuées par cet indep
     *
     * @return une liste de missions
     */
    public static List<Mission> findByZindep(Zindep zindep) {
        return Mission.find("from Mission where zindep = :zindep order by endDate").bind("zindep", zindep).fetch();
    }

    /**
     * Retourne la liste des intermédiares vers la vue pour l'autocompletion
     * dans la page addMision
     *
     * @param term est le terme entré par l'utilisateur.
     * @return une liste qui matche la recherche afin d'afficher une dropdown list.
     */
    public static List<String> getListOfIntermediaries(String term) {
        String whereClause = "";
        if (term != null) {
            whereClause = "where UPPER(intermediary) like UPPER('%" + term + "%') ";
        }
        Query query = JPA.em().createQuery("select distinct intermediary from Mission m " + whereClause + " order by intermediary");
        return query.getResultList();
    }

    public static List<String> getListOfLocations(String term) {
        String whereClause = "";
        if (term != null) {
            whereClause = "where UPPER(location) like UPPER('%" + term + "%') ";
        }
        Query query = JPA.em().createQuery("select distinct location from Mission m " + whereClause + " order by location");
        return query.getResultList();
    }

    public static List<String> getListOfCustomers(String term) {
        String whereClause = "";
        if (term != null) {
            whereClause = "where UPPER(customer) like UPPER('%" + term + "%') ";
        }
        Query query = JPA.em().createQuery("select distinct customer from Mission m " + whereClause + " order by customer");
        return query.getResultList();
    }

    public static List findStatistics(String poste, String intermediary, String customer, String region) {
    	// pas élégant mais ce fut ma seule trouvaille lors du passage à postgresql pour que ca fonctionne
    	poste = poste == null ? "" : poste;
    	intermediary = intermediary == null ? "" : intermediary;
    	customer = customer == null ? "" : customer;
    	region = region == null ? "" : region;
    	
    	Query query = JPA.em().createQuery("select AVG(exp), AVG(clientPrice), AVG(length) from Mission m " +
                "where " +
                " (:role ='' or role = :role)" +
                " AND (:intermediary ='' or intermediary = :intermediary)" +
                " AND (:customer ='' or customer = :customer)" +
                " AND (:region ='' or location = :region)")
                .setParameter("role", poste)
                .setParameter("role", poste)
                .setParameter("intermediary", intermediary)
                .setParameter("intermediary", intermediary)
                .setParameter("customer", customer)
                .setParameter("customer", customer)
                .setParameter("region", region)
                .setParameter("region", region);
        return query.getResultList();
    }

    public static List findPriceByExperience(String poste, String intermediary, String customer, String region) {
    	// pas élégant mais ce fut ma seule trouvaille lors du passage à postgresql pour que ca fonctionne
    	poste = poste == null ? "" : poste;
    	intermediary = intermediary == null ? "" : intermediary;
    	customer = customer == null ? "" : customer;
    	region = region == null ? "" : region;
    	
        Query query = JPA.em().createQuery("select exp, AVG(clientPrice) from Mission m " +
                "where " +
                " (:role ='' or role = :role)" +
                " AND (:intermediary ='' or intermediary = :intermediary)" +
                " AND (:customer ='' or customer = :customer)" +
                " AND (:region ='' or location = :region)" +
                " GROUP BY exp")
                .setParameter("role", poste)
                .setParameter("role", poste)
                .setParameter("intermediary", intermediary)
                .setParameter("intermediary", intermediary)
                .setParameter("customer", customer)
                .setParameter("customer", customer)
                .setParameter("region", region)
                .setParameter("region", region);
        return query.getResultList();
    }

    public String getIntermediaryCommission() {
        BigDecimal intermediaryCommission = new BigDecimal(0);
        intermediaryCommission.setScale(PERCENTAGE_SCALE);
        if (intermediaryPrice != null && intermediaryPrice.longValue() != 0) {
            BigDecimal intermediaryCommissionValue = new BigDecimal(intermediaryPrice - clientPrice);
            BigDecimal divisor = new BigDecimal(intermediaryPrice);
            intermediaryCommission = intermediaryCommissionValue.divide(divisor, PERCENTAGE_SCALE, BigDecimal.ROUND_HALF_UP);
            intermediaryCommission = intermediaryCommission.multiply(new BigDecimal(100));
        }

        return intermediaryCommission.toPlainString();

    }
}
