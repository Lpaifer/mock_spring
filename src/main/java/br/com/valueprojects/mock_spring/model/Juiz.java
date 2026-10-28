
package br.com.valueprojects.mock_spring.model;


public class Juiz {
	
	private double maisPontos = Double.NEGATIVE_INFINITY;
	private double menosPontos = Double.POSITIVE_INFINITY;
	private Participante vencedor;
	
	

	public void julga(Jogo jogo){
		maisPontos = Double.NEGATIVE_INFINITY;
        menosPontos = Double.POSITIVE_INFINITY;
        vencedor = null;

        if (jogo.getResultados().isEmpty()) {
            throw new RuntimeException(
                "Sem resultados não há julgamento!"
            );
        }

        for (Resultado resultado : jogo.getResultados()) {
            if (resultado.getMetrica() > maisPontos) {
                maisPontos = resultado.getMetrica();
                vencedor = resultado.getParticipante();
            }

            if (resultado.getMetrica() < menosPontos) {
                menosPontos = resultado.getMetrica();
            }
        }
	}

	public Participante getVencedor() {
		return vencedor;
	}
		
	public double getPrimeiroColocado(){
			
			return maisPontos;
		}
		
   public double getUltimoColocado(){
			
			return menosPontos;
		}

}
