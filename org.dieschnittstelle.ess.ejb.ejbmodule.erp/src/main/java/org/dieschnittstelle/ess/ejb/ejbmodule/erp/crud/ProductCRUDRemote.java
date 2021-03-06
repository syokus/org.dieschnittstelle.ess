package org.dieschnittstelle.ess.ejb.ejbmodule.erp.crud;

import java.awt.*;
import java.util.List;

import org.dieschnittstelle.ess.entities.erp.AbstractProduct;

import javax.ejb.Remote;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/*
 * TODO JPA3:
 * this interface shall be implemented using a stateless EJB with an EntityManager.
 * See TouchpointCRUDStateless for an example EJB with a similar scope of functionality
 */
@Remote
@Path("/products")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})

public interface ProductCRUDRemote {

	@NotNull
	@POST
	public AbstractProduct createProduct(AbstractProduct prod);
	@NotNull
	@GET
	public List<AbstractProduct> readAllProducts();
	@PUT
	public AbstractProduct updateProduct(AbstractProduct update);
	@GET
	@Path("/{id}")
	public AbstractProduct readProduct(@PathParam("id") long productID);
	@DELETE
	@Path("/{id}")
	public boolean deleteProduct(@PathParam("id") long productID);

}
