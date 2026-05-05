import random
import joblib
from datetime import datetime
import pandas as pd

class PackPredictorService:
    def __init__(self):
        self.model = joblib.load('model.pkl')
        self.random = random.Random()

    def generate_suggestion(self, evenement):
        if not evenement:
            raise ValueError("Un evenement est requis pour generer un pack.")

        titre = self.safe(evenement.get('titre'), 'Evenement')
        categorie = self.safe(evenement.get('categorie'), 'general')
        capacite = max(0, evenement.get('nbPlaces', 0))
        prix = max(0.0, evenement.get('prix', 0.0))

        niveau = self.determiner_niveau()
        nom_pack = f"Pack {niveau} - {titre}"
        montant_suggere = self.calculer_montant(prix, capacite, categorie)
        avantages = self.construire_avantages(evenement, niveau, categorie)

        return PackSuggestion(nom_pack, montant_suggere, avantages)

    def determiner_niveau(self):
        niveaux = ["Standard", "Premium", "Signature", "Or", "Argent", "Platine", "VIP", "Elite", "Exclusif"]
        return random.choice(niveaux)

    def calculer_montant(self, prix, capacite, categorie):
        # Utiliser le modèle ML pour prédire le montant
        df_pred = pd.DataFrame([[categorie, capacite, prix]], columns=['categorie', 'capacite', 'prix'])
        prediction = self.model.predict(df_pred)
        return prediction[0]

    def construire_avantages(self, evenement, niveau, categorie):
        pool = []
        titre = self.safe(evenement.get('titre'), "l'evenement")
        lieu = self.safe(evenement.get('lieu'), evenement.get('adresse', ''))

        pool.extend([
            f"Visibilite du logo sur les supports officiels de {titre}",
            "Mention du sponsor sur les publications reseaux sociaux et le teaser",
            f"Logo present sur les billets et affiches de {titre}",
            "Insertion de flyers ou d'echantillons dans les sacs remis aux participants",
            "Stand d'activation dedie pour rencontrer le public",
            "Acces exclusif aux coulisses ou zones VIP",
            "Invitation a la soiree de lancement ou de cloture",
            "Prise de parole lors de l'inauguration de l'evenement",
            "Remise d'un prix special au nom du sponsor pendant un moment cle",
            "Droits d'utilisation de l'image de l'evenement pour vos campagnes"
        ])

        if lieu:
            pool.extend([
                f"Presence de marque physique sur site a {lieu}",
                f"Signaletique personnalisee a l'entree de {lieu}"
            ])

        normalized_category = categorie.lower()
        if 'mus' in normalized_category:
            pool.extend([
                "Branding scene, backstage ou espace VIP selon disponibilite",
                "Possibilite d'organiser un Meet & Greet exclusif avec les artistes"
            ])
        elif 'sport' in normalized_category:
            pool.extend([
                "Visibilite forte sur les zones d'animation, dossards ou podium",
                "Naming officiel d'une epreuve specifique de l'evenement"
            ])
        elif 'thea' in normalized_category or 'th' in normalized_category:
            pool.append("Insertion prioritaire du sponsor dans le programme et l'accueil public")
        else:
            pool.append("Emplacement de choix pour votre stand d'activation")

        # Mélanger les avantages et en choisir 3, 4 ou 5 aléatoirement
        random.shuffle(pool)
        nb_avantages = 3 + random.randint(0, 2)
        selection = pool[:nb_avantages]

        # Si le niveau est premium/or/signature, ajouter un bonus
        if niveau in ["Signature", "Platine", "VIP"]:
            selection.insert(0, "Statut de Partenaire Officiel exclusif")  # Ajouter en premier

        # Ajouter toujours la date de validité si elle existe
        if evenement.get('dateEvenement'):
            # Assumer que dateEvenement est une chaîne au format 'YYYY-MM-DD'
            try:
                date_obj = datetime.strptime(evenement['dateEvenement'], '%Y-%m-%d')
                date = date_obj.strftime('%d/%m/%Y')
                selection.append(f"Operation de visibilite valable pour l'evenement du {date}")
            except ValueError:
                # Si le format n'est pas correct, ignorer
                pass

        return "- " + "\n- ".join(selection)

    def safe(self, value, fallback):
        if value is None:
            return fallback
        trimmed = str(value).strip()
        return trimmed if trimmed else fallback

class PackSuggestion:
    def __init__(self, nom, montant, avantages):
        self.nom = nom
        self.montant = montant
        self.avantages = avantages

    def get_nom(self):
        return self.nom

    def get_montant(self):
        return self.montant

    def get_avantages(self):
        return self.avantages

