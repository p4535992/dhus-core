/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2014-2018 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gael.dhus.olingo.v1.entity;

import fr.gael.dhus.olingo.v1.entityset.ProductEntitySet;
import fr.gael.dhus.spring.context.ApplicationContextProvider;

import fr.gael.drb.DrbNode;
import fr.gael.drb.DrbSequence;
import fr.gael.drb.query.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dhus.store.StoreException;
import org.dhus.store.datastore.DataStoreException;
import org.dhus.store.datastore.DataStoreProduct;
import org.dhus.store.derived.DerivedProductStore;
import org.dhus.store.derived.DerivedProductStoreService;

/**
 * @author pidancier
 */
public class ThumbnailProduct extends Product
{
   // xpath_attributes is a set of xpath returning nodes (ClasscastException
   // overwise)
   private static String[] xpath_attributes = { "image/FormatName",
         "image/directory/Width", "image/directory/Height",
         "image/directory/NumBands" };

   private static final DerivedProductStoreService DERIVED_PS_SERVICE = ApplicationContextProvider.getBean(DerivedProductStoreService.class);

   private final DataStoreProduct physical;

   static ThumbnailProduct generateThumbnailProduct(fr.gael.dhus.database.object.Product product)
   {
      DataStoreProduct data = null;
      try
      {
         data = DERIVED_PS_SERVICE
               .getDerivedProduct(product.getUuid(),DerivedProductStore.THUMBNAIL_TAG)
               .getImpl(DataStoreProduct.class);
      }
      catch (StoreException suppressed) {}
      return new ThumbnailProduct(product, data);
   }

   /**
    * Build this Thumbnail instance.
    *
    * @param product
    * @param data physical product, may be null
    * @throws NullPointerException is product contains no Quicklook.
    * @throws DataStoreException
    */
   private ThumbnailProduct(fr.gael.dhus.database.object.Product product, DataStoreProduct data)
   {
      super (product);
      this.physical = data;
   }

   @Override
   public String getId ()
   {
      return "Thumbnail";
   }

   @Override
   public String getName ()
   {
      return "THUMB" + product.getIdentifier ();
   }

   @Override
   public Long getContentLength ()
   {
      return product.getThumbnailSize ();
   }

   @Override
   public boolean requiresControl ()
   {
      return false;
   }

   @Override
   public Map<String, Product> getProducts ()
   {
      return Collections.emptyMap();
   }

   @Override
   public Map<String, Node> getNodes ()
   {
      if (this.nodes == null)
      {
         Map<String, Node> nodes = new LinkedHashMap<>();
         if (physical != null)
         {
            DrbNode parent = physical.getImpl(DrbNode.class);
            if (parent != null)
            {
               nodes.put(parent.getName(), new Node(parent));
            }
         }

         this.nodes = nodes;
      }
      return this.nodes;
   }

   /**
    * The returned list is immutable.
    */
   @Override
   public Map<String, Attribute> getAttributes ()
   {
      if (this.attributes == null)
      {
         Map<String, Attribute> attributes = new LinkedHashMap<>();
         DrbNode node = null;
         if (physical != null)
         {
            node = physical.getImpl(DrbNode.class);
         }

         for (String xpath : xpath_attributes)
         {
            Query query = new Query (xpath);
            DrbSequence results = query.evaluate (node);
            if ( (results != null) && (results.getLength () > 0))
            {
               DrbNode result = (DrbNode) results.getItem (0);
               if (result.getValue () != null)
                  attributes.put (result.getName (),
                     new Attribute(result.getName(), result.getValue().toString(), null));
            }
         }
         this.attributes = attributes;
      }
      return this.attributes;
   }

   @Override
   public InputStream getInputStream() throws IOException
   {
      try
      {
         return physical.getImpl(InputStream.class);
      }
      catch (Exception e)
      {
         throw new IOException("Cannot get thumbnail from product", e);
      }
   }

   @Override
   public Map<String, Object> toEntityResponse(String root_url)
   {
      Map<String, Object> response = super.toEntityResponse(root_url);
      if (physical != null)
      {
         response.put(ProductEntitySet.LOCAL_PATH, physical.getResourceLocation());
      }
      return response;
   }

   @Override
   protected DataStoreProduct getPhysicalProduct() throws DataStoreException
   {
      return physical;
   }
}
