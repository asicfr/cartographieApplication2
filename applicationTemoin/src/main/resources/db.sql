--
-- Table structure for table `clients`
--

DROP TABLE IF EXISTS `clients`;
CREATE TABLE IF NOT EXISTS `clients` (
  `id` char(5) NOT NULL,
  `nom` varchar(50) DEFAULT NULL,
  `prenom` varchar(30) DEFAULT NULL,
  `date_naissance` date DEFAULT NULL,
  `e_mail` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `commandes`
--

DROP TABLE IF EXISTS `commandes`;
CREATE TABLE IF NOT EXISTS `commandes` (
  `id` char(5) NOT NULL,
  `client_id` char(5) NOT NULL,
  `produit_id` char(5) DEFAULT NULL,
  `date_commande` date DEFAULT NULL,
  PRIMARY KEY (`id`,`client_id`),
  KEY `client_id` (`client_id`),
  KEY `produit_id` (`produit_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `produits`
--

DROP TABLE IF EXISTS `produits`;
CREATE TABLE IF NOT EXISTS `produits` (
  `id` char(5) NOT NULL,
  `titre` varchar(50) DEFAULT NULL,
  `prix` decimal(6,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
COMMIT;