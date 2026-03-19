package de.bericht.service;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

@FacesValidator("zahlValidator")
public class ZahlValidator implements Validator {

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		KiAenderung a = (KiAenderung) component.getAttributes().get("kiAenderung");
		int zahl = (Integer) value;
		if (zahl < a.getBereich_min() || zahl > a.getBereich_max()) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Zahl muss zwischen " + a.getBereich_min() + " und " + a.getBereich_max() + " liegen."));
		}
		int step = a.getBereich_schritt();
		if ((zahl - a.getBereich_min()) % step != 0) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
					"Zahl muss ein Vielfaches von " + step + " ab dem Minimum sein."));
		}
	}
}
