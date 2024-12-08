package com.ibs.steps;

import io.cucumber.java.ru.Дано;
import io.cucumber.java.ru.И;
import io.cucumber.java.ru.Когда;
import io.cucumber.java.ru.Тогда;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class TestSandBoxSteps {

    private WebDriver driver;
    private Connection connection;

    @Дано("^открыть страницу \"([^\"]*)\"$")
    public void открыть_страницу(String url) {
        // Настройка WebDriver
        driver = new ChromeDriver();
        System.setProperty("webdriver.chromedriver.driver", "src/test/resources/chromedriver.exe");// подключение драйвера
        driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();// дисплей на максимум

        driver.get(url);  // открыли страницу
    }

    @Когда("^переход в раздел \"([^\"]*)\"$")
    public void перед_в_раздел(String section) {
        WebElement sandBox = driver.findElement(By.xpath("//a[@class ='nav-link dropdown-toggle']"));
        sandBox.click(); // клик на песочницу

        WebElement goods = driver.findElement(By.xpath("//a[text()='" + section + "']"));
        goods.click(); // клик на товары
    }


    @И("^ввод название товара \"([^\"]*)\"$")
    public void ввод_название_товара(String productName) {
        WebElement productNameField = driver.findElement(By.xpath("//*[@id='name']"));
        productNameField.sendKeys(productName);// Ввести значение в текстовое поле
    }

    @И("^выбор тип товара \"([^\"]*)\"$")
    public void выбор_тип_товара(String type) {
        WebElement typeField = driver.findElement(By.xpath("//*[@id='type']"));
        typeField.click();  // клик на поле тип

        WebElement typeOption = driver.findElement(By.xpath("//option[@value='" + type.toUpperCase() + "']"));
        typeOption.click(); // клик по типу фрукт
    }

    @И("^нажать на кнопку \"([^\"]*)\"$")
    public void нажать_на_кнопку(String buttonText) {
        WebElement buttonAdd = driver.findElement(By.xpath("//button[text()='" + buttonText + "']"));
        buttonAdd.click(); // клик на кнопку добавить
    }

    @Тогда("^видим товар \"([^\"]*)\" в списке товаров$")
    public void видим_товар_в_списке_товаров(String productName) {
        driver.findElement(By.xpath("//th[.='5']/following-sibling::td[.='" + productName + "']")).isDisplayed();
    }

    @И("^количество товаров в базе данных$")
    public void количество_товаров_в_базе_данных_равно() throws SQLException {
        // устанавливаем соединение с БД, передали УРЛ, логин и пароль
        connection = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mem:testdb",
                "user", "pass");

        // Проверка количества товаров в БД
        String query = "SELECT FOOD_NAME, COUNT(FOOD_NAME) AS COUNT FROM food " +
                "WHERE food_name = 'Яблоко' AND FOOD_TYPE = 'FRUIT' AND FOOD_EXOTIC = '0' GROUP BY FOOD_NAME";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        while (resultSet.next()) {
            String name = resultSet.getString("FOOD_NAME");
            int count = resultSet.getInt("COUNT");
            System.out.printf("%s, %d%n", name, count);
        }
    }

    @Когда("^удаляем товар \"([^\"]*)\" из базы данных$")
    public void удалить_товар_из_базы_данных(String productName) throws SQLException {
        // Удаление товара через БД
        String deleteQuery = "DELETE FROM food WHERE food_name = '" + productName + "';";
        Statement statement = connection.createStatement();
        int rowsDeleted = statement.executeUpdate(deleteQuery);
        System.out.println("Удалено строк: " + rowsDeleted);
    }

    @Тогда("^количество товаров \"([^\"]*)\" в базе данных после удаления равно (\\d+)$")
    public void количество_товаров_в_базе_данных_равно_после_удаления(String productName, int count) throws SQLException {
        // Проверка количества товаров после удаления
        String query = "SELECT FOOD_NAME, COUNT(FOOD_NAME) AS COUNT FROM food " +
                "WHERE food_name = '" + productName + "' AND FOOD_TYPE = 'FRUIT' AND FOOD_EXOTIC = '0' GROUP BY FOOD_NAME";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        if (resultSet.next()) {
            int actualCount = resultSet.getInt("COUNT");
            if (actualCount != count) {
                throw new AssertionError("Ожидаемое количество товаров: " + count + ", фактическое: " + actualCount);
            }
        } else {
            if (count != 0) {
                throw new AssertionError("Ожидаемое количество товаров: " + count + ", фактическое: 0");
            }
        }

        connection.close();
        driver.quit();
    }
}
