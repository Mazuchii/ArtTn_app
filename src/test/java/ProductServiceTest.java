import tn.esprit.museum.entities.Product;
import tn.esprit.museum.services.ProductService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductServiceTest {

    static ProductService ps;
    static int testId;

    @BeforeAll
    static void init() {
        ps = new ProductService();
        System.out.println("Initialisation terminée");
    }


    @Test
    public void testerAjout() {
        try {
            Product p = new Product("Produit Test", "Description test", 49.99, 5, "Livres", "/images/test.jpg");
            ps.insert(p);

            assertTrue(p.getId() != 0);
            testId = p.getId();

            System.out.println("Test ajout réussi - ID: " + testId);

        } catch (SQLException e) {
            fail("Erreur: " + e.getMessage());
        }
    }

    @Test
    public void testerAffichage() {
        try {
            List<Product> produits = ps.getAll();
            assertNotNull(produits);
            System.out.println("Test affichage réussi - " + produits.size() + " produits");
        } catch (SQLException e) {
            fail("Erreur: " + e.getMessage());
        }
    }

    @Test
    public void testerModification() {
        try {
            // Créer un produit pour le test
            Product p = new Product("A Modifier", "Description", 25.00, 10, "Coffrets", null);
            ps.insert(p);
            int id = p.getId();

            // Modifier
            p.setName("Produit Modifié");
            p.setPrice(35.00);
            ps.update(p);

            // Vérifier
            Product modified = ps.getById(id);
            assertEquals("Produit Modifié", modified.getName());
            assertEquals(35.00, modified.getPrice());

            System.out.println(" Test modification réussi");

        } catch (SQLException e) {
            fail("Erreur: " + e.getMessage());
        }
    }

    @Test
    public void testerSuppression() {
        try {
            // Créer un produit pour le test
            Product p = new Product("A Supprimer", "Description", 15.00, 3, "Accessoires", null);
            ps.insert(p);
            int id = p.getId();

            // Supprimer
            ps.delete(id);

            // Vérifier
            Product deleted = ps.getById(id);
            assertNull(deleted);

            System.out.println(" Test suppression réussi");

        } catch (SQLException e) {
            fail("Erreur: " + e.getMessage());
        }
    }
}