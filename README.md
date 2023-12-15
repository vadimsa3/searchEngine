<h1 align="center">ЛОКАЛЬНЫЙ ПОИСКОВЫЙ ДВИЖОК ПО САЙТУ</h1>
<h2 align="center"><img src="https://github.com/vadimsa3/searchengine/blob/master/src/main/resources/raw/target.gif" height="32"/>
<a href="https://github.com/vadimsa3/searchengine/tree/master/src/main/java/searchengine" target="_blank">Переход к коду</a></h2>
<h2 align="center">Поисковая система для проведения индексации группы сайтов с последущим поиском контента (на русском языке) по содержимому сайтов.</h2>

![Изображение](https://github.com/vadimsa3/searchengine/blob/master/src/main/resources/raw/demo.png "Внешний вид")

## **СОДЕРЖАНИЕ:** ##
* [ТЕХНОЛОГИИ](#технологии)  
* [ИНСТРУКЦИЯ ПО ЗАПУСКУ](#инструкция_по_запуску)
## **СТЭК ТЕХНОЛОГИЙ** ## 
<a name="технологии"></a>
Синтаксис *JAVA 18*.

Фреймворк *Spring Boot v2.7.1* для упрощения создания автономных приложений на базе Spring. 

Реляционная система управления базами данных *MySQL 8.0*.

*Библиотеки:*

*JSOUP 1.16.1* для анализа, извлечения и обработки данных, хранящихся в HTML-документах.

*Lombok 1.18.30* для упрощения (исключения) шаблонного Java-кода через аннотации.

*Apache Lucene Morphology 1.5* для проведения морфологического анализа слов.

*log4j 1.2.17* для журналирования (логирования) Java-программ.

*Thymeleaf* шаблонизатор Java для обработки и создания HTML, XML, JavaScript, CSS и текста без серверной части.

## **ИНСТРУКЦИЯ ПО ЗАПУСКУ** ##
<a name="инструкция_по_запуску"></a> 
**1. Проверьте установлены-ли следующие компоненты и установите их при отсутствии:**
* Java Development Kit (JDK) версии 17 или выше.
* Apache Maven фреймворк для автоматизации сборки проектов.
* MySQL 8.0.26 или выше.

**2. Клонируйте репозиторий.**  

Клонируйте репозиторий с кодом проекта на свой локальный компьютер:  
git clone https://github.com/vadimsa3/searchengine/

**3. Создайте реляционную базу данных с Вашими правами доступа.**  

**4. Настройте Ваш файл конфигурации application.yaml.**  

* Откорректируйте имя пользователя (username) и пароль (password) для подключения к Вашей базе данных с соответствующими правами доступа.
* Внесите наименование Вашей базы данных в:  
* jdbc:mysql://localhost:3306/your_database_name?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&character_set_server=utf8mb4
* Введите URL-адреса и названия сайтов для проведения их индексации.

**5. Настройте Ваш файл конфигурации application.yaml.**  
