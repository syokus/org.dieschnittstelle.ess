package org.dieschnittstelle.ess.ejb.ejbmodule.erp;

import org.dieschnittstelle.ess.ejb.ejbmodule.erp.crud.PointOfSaleCRUDLocal;
import org.dieschnittstelle.ess.ejb.ejbmodule.erp.crud.StockItemCRUDLocal;
import org.dieschnittstelle.ess.entities.erp.IndividualisedProductItem;
import org.dieschnittstelle.ess.entities.erp.PointOfSale;
import org.dieschnittstelle.ess.entities.erp.StockItem;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class StockSystemSingleton implements StockSystemLocal{

    @EJB
    private StockItemCRUDLocal siCrud;

    @EJB
    private PointOfSaleCRUDLocal posCrud;

    @Override
    public void addToStock(IndividualisedProductItem product, long pointOfSaleId, int units) {
        System.out.println("addToStock(): siCrud: " + siCrud + "of class: " + siCrud.getClass());
        System.out.println("addToStock(): posCrud: " + posCrud + "of class: " + posCrud.getClass());

        PointOfSale pos = posCrud.readPointOfSale(pointOfSaleId);

        StockItem stockItem = siCrud.readStockItem(product, pos);
        if (stockItem == null) {
            stockItem = new StockItem(product,pos,units);
            siCrud.createStockItem(stockItem);
        }
        else {
            stockItem.setUnits(stockItem
            .getUnits()+units);
            siCrud.updateStockItem(stockItem);
        }
    }

    @Override
    public void removeFromStock(IndividualisedProductItem product, long pointOfSaleId, int units) {
        addToStock(product,pointOfSaleId,-units);
    }

    @Override
    public List<IndividualisedProductItem> getProductsOnStock(long pointOfSaleId) {
/*        List<StockItem> silist = siCrud
                .readStockItemsForPointOfSale(posCrud.readPointOfSale(pointOfSaleId));
        List<IndividualisedProductItem> productItems = new ArrayList<>();
        for (StockItem si : silist) {
            productItems.add(si.getProduct());
        }
        return productItems;*/



        return new ArrayList<>(siCrud
                .readStockItemsForPointOfSale(posCrud.readPointOfSale(pointOfSaleId))
                .stream()
                .map(si -> si.getProduct())
                .collect(Collectors.toSet()));
    }

    @Override
    public List<IndividualisedProductItem> getAllProductsOnStock() {
        return new ArrayList<>(new HashSet<>());
    }

    @Override
    public int getUnitsOnStock(IndividualisedProductItem product, long pointOfSaleId) {
        return 0;
    }

    @Override
    public int getTotalUnitsOnStock(IndividualisedProductItem product) {
        List<StockItem> stockItems = siCrud.readStockItemsForProduct(product);
        return stockItems.stream()
                .mapToInt(si -> si.getUnits())
                .sum(); //lambda

    }

    @Override
    public List<Long> getPointsOfSale(IndividualisedProductItem product) {
        return null;
    }

    @Override
    public List<StockItem> getCompleteStock() {
        throw new UnsupportedOperationException("getCompleteStock() is not Supported");
    }
}
