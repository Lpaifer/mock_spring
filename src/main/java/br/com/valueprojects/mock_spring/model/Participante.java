package br.com.valueprojects.mock_spring.model;

public class Participante {

	private int id;
	private String nome;
	private String telefone;
	
	public Participante(String nome) {
		this(0, nome);
	}

	public Participante(int id, String nome) {
		this(id, nome, "");
	}

	public Participante(int id, String nome, String telefone) {
		this.id = id;
		this.nome = nome;
		this.telefone = telefone;
	}

	public String getTelefone() {
		return telefone;
	}

	public int getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Participante other = (Participante) obj;
		if (id != other.id)
			return false;
		if (nome == null) {
			if (other.nome != null)
				return false;
		} else if (!nome.equals(other.nome))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, nome);
	}
	
	
}
