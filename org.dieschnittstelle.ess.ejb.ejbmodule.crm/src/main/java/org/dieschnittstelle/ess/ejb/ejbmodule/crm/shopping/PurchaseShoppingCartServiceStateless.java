package org.dieschnittstelle.ess.ejb.ejbmodule.crm.shopping;

import org.apache.logging.log4j.Logger;
import org.dieschnittstelle.ess.ejb.ejbmodule.crm.*;
import org.dieschnittstelle.ess.ejb.ejbmodule.crm.crud.CustomerCRUDLocal;
import org.dieschnittstelle.ess.ejb.ejbmodule.crm.crud.TouchpointCRUDLocal;
import org.dieschnittstelle.ess.ejb.ejbmodule.erp.StockSystemLocal;
import org.dieschnittstelle.ess.entities.crm.AbstractTouchpoint;
import org.dieschnittstelle.ess.entities.crm.Customer;
import org.dieschnittstelle.ess.entities.crm.CustomerTransaction;
import org.dieschnittstelle.ess.entities.crm.ShoppingCartItem;
import org.dieschnittstelle.ess.entities.erp.AbstractProduct;
import org.dieschnittstelle.ess.entities.erp.Campaign;
import org.dieschnittstelle.ess.entities.erp.IndividualisedProductItem;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class PurchaseShoppingCartServiceStateless implements PurchaseShoppingCartService {

    protected static Logger logger = org.apache.logging.log4j.LogManager.getLogger(PurchaseShoppingCartServiceStateless.class);

    /*
     * the three beans that are used
     */

    // 13.07.2020 Sioki
    // ESS 10 PAT-3 Demo PAT1 - teil 2 dakkika 37 Ubungla alakali
    // purchase welche hier ganz unten Steht muss umbenannt werden Refaktor- Interface Client seite alles umbennant werden

    private ShoppingCartRemote shoppingCart;

    @EJB(beanName = "ShoppingCartRESTServiceImpl")
    private ShoppingCartServiceLocal shoppingCartServiceLocal;

    @EJB
    private CustomerTrackingRemote customerTracking;

    @EJB
    private CampaignTrackingRemote campaignTracking;

    @EJB
    private CustomerCRUDLocal customerCRUDLocal;

    @EJB
    private TouchpointCRUDLocal touchpointCRUDLocal;

    @EJB
    private StockSystemLocal stockSystemLocal;

    /**
     * the customer
     */
    private Customer customer;

    /**
     * the touchpoint
     */
    private AbstractTouchpoint touchpoint;

    public void setTouchpoint(AbstractTouchpoint touchpoint) {
        this.touchpoint = touchpoint;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void addProduct(AbstractProduct product, int units) {
        this.shoppingCart.addItem(new ShoppingCartItem(product.getId(), units, product instanceof Campaign));
    }

    /*
     * verify whether campaigns are still valid
     */
    public void verifyCampaigns() throws ShoppingException {
        if (this.customer == null || this.touchpoint == null) {
            throw new RuntimeException("cannot verify campaigns! No touchpoint has been set!");
        }

        for (ShoppingCartItem item : this.shoppingCart.getItems()) {
            if (item.isCampaign()) {
                int availableCampaigns = this.campaignTracking.existsValidCampaignExecutionAtTouchpoint(
                        item.getErpProductId(), this.touchpoint);
                logger.info("got available campaigns for product " + item.getErpProductId() + ": "
                        + availableCampaigns);
                // we check whether we have sufficient campaign items available
                if (availableCampaigns < item.getUnits()) {
                    throw new ShoppingException("verifyCampaigns() failed for productBundle " + item
                            + " at touchpoint " + this.touchpoint + "! Need " + item.getUnits()
                            + " instances of campaign, but only got: " + availableCampaigns);
                }
            }
        }
    }

    public void purchaseNow()  throws ShoppingException {
        logger.info("purchase()");

        if (this.customer == null || this.touchpoint == null) {
            throw new RuntimeException(
                    "cannot commit shopping session! Either customer or touchpoint has not been set: " + this.customer
                            + "/" + this.touchpoint);
        }

        // verify the campaigns
        verifyCampaigns();

        // remove the products from stock
        checkAndRemoveProductsFromStock();

        // then we add a new customer transaction for the current purchase
        List<ShoppingCartItem> products = this.shoppingCart.getItems();
        CustomerTransaction transaction = new CustomerTransaction(this.customer, this.touchpoint, products);
        transaction.setCompleted(true);
        customerTracking.createTransaction(transaction);

        logger.info("purchase(): done.\n");
    }

    /*
     * to be implemented as server-side method for PAT2
     */
    private void checkAndRemoveProductsFromStock() {
        logger.info("checkAndRemoveProductsFromStock");

        for (ShoppingCartItem item : this.shoppingCart.getItems()) {

            // TODO: ermitteln Sie das AbstractProduct für das gegebene ShoppingCartItem. Nutzen Sie dafür dessen erpProductId und die ProductCRUD EJB

            if (item.isCampaign()) {
                this.campaignTracking.purchaseCampaignAtTouchpoint(item.getErpProductId(), this.touchpoint,
                        item.getUnits());
                // TODO: wenn Sie eine Kampagne haben, muessen Sie hier
                // 1) ueber die ProductBundle Objekte auf dem Campaign Objekt iterieren, und
                // 2) fuer jedes ProductBundle das betreffende Produkt in der auf dem Bundle angegebenen Anzahl, multipliziert mit dem Wert von
                // item.getUnits() aus dem Warenkorb,
                // - hinsichtlich Verfuegbarkeit ueberpruefen, und
                // - falls verfuegbar, aus dem Warenlager entfernen - nutzen Sie dafür die StockSystem EJB
                // (Anm.: item.getUnits() gibt Ihnen Auskunft darüber, wie oft ein Produkt, im vorliegenden Fall eine Kampagne, im
                // Warenkorb liegt)
                List<IndividualisedProductItem> itemsOnStock = this.stockSystemLocal.getProductsOnStock(item.getId());
                int amount = 0;
                for(IndividualisedProductItem individualisedProductItem : itemsOnStock) {
                    if(individualisedProductItem.getId() == item.getId()) {
                        amount++;
                    }
                }
                if(amount == item.getUnits()) {
                    for(IndividualisedProductItem individualisedProductItem : itemsOnStock) {
                        if(individualisedProductItem.getId() == item.getId()) {
                            this.stockSystemLocal.removeFromStock(individualisedProductItem, individualisedProductItem.getId(), item.getUnits());
                        }
                    }
                }
            } else {
                // TODO: andernfalls (wenn keine Kampagne vorliegt) muessen Sie
                // 1) das Produkt in der in item.getUnits() angegebenen Anzahl hinsichtlich Verfuegbarkeit ueberpruefen und
                // 2) das Produkt, falls verfuegbar, in der entsprechenden Anzahl aus dem Warenlager entfernen
                List<IndividualisedProductItem> itemsOnStock = this.stockSystemLocal.getProductsOnStock(item.getId());
                int amount = 0;
                for(IndividualisedProductItem individualisedProductItem : itemsOnStock) {
                    if(individualisedProductItem.getId() == item.getId()) {
                        amount++;
                    }
                }
                if(amount == item.getUnits()) {
                    for(IndividualisedProductItem individualisedProductItem : itemsOnStock) {
                        this.stockSystemLocal.removeFromStock(individualisedProductItem, individualisedProductItem.getId(), item.getUnits());
                    }
                }
            }

        }
    }

    @Override
    public void purchase(long shoppingCartId, long touchpointId, long customerId) throws ShoppingException {
        this.customer = customerCRUDLocal.readCustomer(customerId);
        this.touchpoint = touchpointCRUDLocal.readTouchpoint(touchpointId);
        this.shoppingCart = shoppingCartServiceLocal.getCartForId(shoppingCartId);

        purchaseNow();

        // cleanup
//        this.customer = null;
//        this.touchpoint = null;
//        this.shoppingCart = null;
    }

}
