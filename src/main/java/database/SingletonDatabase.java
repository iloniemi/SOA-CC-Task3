package database;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import errors.DataNotFoundException;
import errors.HandlingException;
import types.Customer;
import types.Order;
import types.Product;
import types.ProductCategory;
import types.ShopData;

/**
 * Reads the JSON-file and acts as a dumb database for the web shop
 * Other classes can refer to the database by  SingletonDatabase.getDatabase().getCustomers() and SingletonDatabase.getDatabase().GetProductCategories()
 */
public class SingletonDatabase {
	private List<Customer> savedCustomers;
	private List<ProductCategory> savedProductCategories;

	static SingletonDatabase obj = new SingletonDatabase();

	private SingletonDatabase() {

		if (savedProductCategories == null) {
			try {
				//Käytän vähän spagettikoodimaisesti Jukan ShopData luokan Get-metodeja JSON-tiedoston lukemisessa
				
				ObjectMapper mapper = new ObjectMapper();
				InputStream is = ShopData.class.getResourceAsStream("/shopdata.json");
				
				ShopData shopData = mapper.readValue(is, ShopData.class);
				
				savedCustomers = shopData.getCustomers();
				savedProductCategories = shopData.getProductCategories();
			} catch (Exception e) {
				e.printStackTrace();
				savedProductCategories = null;
			}
		}

	}

	public static SingletonDatabase getDatabase() {
		return obj;
	}


	//----Category------------
	
	/**
	 * Function returns the list of product categories
	 * @return list of product categories or throws an exception if there was an error
	 */
	public List<ProductCategory> getProductCategories() {
		if (savedProductCategories == null) {
			throw new HandlingException("Could not get categories");
		}
		return savedProductCategories;
	}

	/**
	 * Function returns the requested product category if found
	 * @param categoryId id to find
	 * @return ProductCategory with categoryId, exception if not found (or if there was an error)
	 */
	public ProductCategory getCategory(String categoryId) {
		if (savedProductCategories == null) {
			throw new DataNotFoundException("Category with id "+ categoryId + " not found");
		}
		ProductCategory category = savedProductCategories.stream()
				.filter(cat -> cat.getId().equals(categoryId))
				.findFirst()
				.orElse(null);
		if (category == null) {
			throw new DataNotFoundException("Category with id "+ categoryId + " not found");
		}
		return category;
	}
	
	/**
	 * Function to add a new category
	 * @param category category object to be added
	 * @return Added category object (or exception if there was an error)
	 */
	public ProductCategory addCategory(ProductCategory newCategory) {
		if (savedProductCategories == null) {
			throw new DataNotFoundException("Problem adding category "+ newCategory);
		}
		
		savedProductCategories.add(newCategory);
		return newCategory;
	}
	
	/**
	 * Replace/edit a category
	 * @param cateogryId id to be replaced
	 * @param newCateogry category that replaces old category
	 * @return replaced category, throws exception if not found (or if there was an error)
	 */
	public ProductCategory replaceCategory(String categoryId, ProductCategory newCategory) {
		ProductCategory category = this.getCategory(categoryId);
		
		if (category == null) {
			throw new HandlingException("Category with id "+ categoryId + " could not be replaced");
		}
		
			category.setName(newCategory.getName());
		
			//Products replaced only if there are new products
			if (!newCategory.getProducts().isEmpty()) {
				category.setProducts(newCategory.getProducts());				
			}
			
			return category;
	}
	
	/**
	 * Function to remove product categories
	 * @param categoryId id to be removed
	 * @return  true if found and succeeded
	 */
	public boolean removeCategory(String categoryId) {
		if (savedProductCategories == null) {
			throw new HandlingException("Category with id "+ categoryId + " not removed");
		}
		return savedProductCategories.removeIf(category -> category.getId().equals(categoryId));
	}

	//----------Product----------------------------
	
	/** Get all products in a category of specified id.
	 * @param categoryId id of the category whose products will be returned.
	 * @param maker search string for n
	 * @return List of products in the category.
	 */
	public List<Product> getProducts(String categoryId, String manufacturer) {
		ProductCategory category = this.getCategory(categoryId);
		String manufacturerRegex;
		if (manufacturer != null) {
			manufacturerRegex = ".*" + manufacturer + ".*";
		} else {
			manufacturerRegex = ".*";
		}
		
		List<Product> products = category.getProducts().stream()
				.filter(product -> product.getManufacturer().matches(manufacturerRegex))
				.collect(Collectors.toList());
		return products;
	}
	
	/** Finds product with given id.
	 * @param productId 
	 * @return Product with given id or null if not found
	 */
	public Product getProduct(String categoryId, String productId) {
		ProductCategory category = this.getCategory(categoryId);
		
		Product product = category.getProducts().stream()
				.filter(prod -> productId.equals(prod.getId()))
				.findFirst()
				.orElse(null);
		
		if (product == null) {
			throw new DataNotFoundException("Product with id "+ productId + " not found");
		}
		
		return product;
	}
	
	
	/** Adds a product to a specified category.
	 * @param categoryId
	 * @param product
	 * @return Added product
	 */
	public Product addProduct(String categoryId, Product product) {
		ProductCategory category = this.getCategory(categoryId);
		return category.addProduct(product);
	}

	public Product replaceProduct(String categoryId, String productId, Product product) {
		ProductCategory category = this.getCategory(categoryId);
		Product productToBeUpdated = category.getProduct(productId);
		return productToBeUpdated.update(product);
	}
	
	/** Delete a product from a category with corresponding ids
	 * @param categoryId
	 * @param productId
	 * @return was a product successfully deleted
	 */
	public boolean removeProduct(String categoryId, String productId) {
		ProductCategory category = this.getCategory(categoryId);
		return category.getProducts().removeIf(product -> productId.equals(product.getId()));
	}
	
	
	//------Customer--------------------------
	
	/**
	 * @return list of customers
	 */
	public List<Customer> getCustomers() {
		return savedCustomers;
	}
	
	/**
	 * 
	 * @param customerId id to find
	 * @return customer with customerId, null if not found (or if there was an error)
	 */
	public Customer getCustomer(String customerId) {
		if (savedCustomers != null) {
			return savedCustomers.stream().filter(customer -> customer.getId().equals(customerId)).findFirst().orElse(null);
		}
		return null;
	}
	
	
	/**
	 * 
	 * @param customer customer object to be added
	 * @return Added customer object (or null if there was an error)
	 */
	public Customer addCustomer(Customer customer) {
		if (savedCustomers != null) {
			savedCustomers.add(customer);
			return customer;
		}
		return null;
	}
	
	/**
	 * Replaces customer info, if new orders are empty, old ones will be used
	 * @param customerId id to be replaced
	 * @param newCustomer customer to be replaced by
	 * @return replaced customer, null if not found (or if there was an error)
	 */
	public Customer replaceCustomer(String customerId, Customer newCustomer) {
		Customer customer = this.getCustomer(customerId);
		if (customer != null) {
			customer.setFirstName(newCustomer.getFirstName());
			customer.setLastName(newCustomer.getLastName());
			
			//if new customer info does not contain new orders old ones will not change
			if (!newCustomer.getOrders().isEmpty()) {
				customer.setOrders(newCustomer.getOrders());
			}
			
			return customer;
		}
		return null;
	}
	
	/**
	 * 
	 * @param customerId id to be removed
	 * @return removed true if found and succeed
	 */
	public boolean removeCustomer(String customerId) {
		if (savedCustomers != null) {
			return savedCustomers.removeIf(customer -> customer.getId().equals(customerId));
		}
		return false;
	}
	
	
	//----Order--------------------
	
	/** Get all products in a category of specified id.
	 * @param customerId id of the category whose products will be returned.
	 * @return List of orders in the category.
	 */
	public List<Order> getOrders(String customerId) {
		Customer customer = this.getCustomer(customerId);
		return customer.getOrders();
	}
	
	/** Finds order with given id.
	 * @param orderId 
	 * @return order with given id or null if not found
	 */
	public Order getOrder(String customerId, String orderId) {
		Customer customer = this.getCustomer(customerId);
		
		Order order = customer.getOrders().stream()
				.filter(prod -> orderId.equals(prod.getId()))
				.findFirst()
				.orElse(null);
		
		if (order == null) {
			throw new DataNotFoundException("Order with id "+ orderId + " not found");
		}
		
		return order;
	}
	
	
	/** Adds a order to a specified customer.
	 * @param customerId
	 * @param order
	 * @return Added order
	 */
	public Order addOrder(String customerId, Order order) {
		Customer customer = this.getCustomer(customerId);
		return customer.addOrder(order);
	}

	public Order replaceOrder(String customerId, String orderId, Order order) {
		Customer customer = this.getCustomer(customerId);
		Order orderToBeUpdated = customer.getOrder(orderId);
		return orderToBeUpdated.update(order);
	}
	
	/** Delete a order from a customer with corresponding ids
	 * @param customerId
	 * @param orderId
	 * @return was a order successfully deleted
	 */
	public boolean removeOrder(String customerId, String orderId) {
		Customer customer = this.getCustomer(customerId);
		return customer.getOrders().removeIf(order -> orderId.equals(order.getId()));
	}
}