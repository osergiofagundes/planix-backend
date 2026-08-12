# Documentação do Planix

Comece pelo [`CLAUDE.md`](../CLAUDE.md) na raiz: ele tem as regras que valem
sempre. Estes documentos são o detalhe.

| Documento | Abra quando… |
|---|---|
| [ARQUITETURA.md](ARQUITETURA.md) | quiser entender as camadas, por que os pacotes são organizados assim e por onde um request passa até o banco |
| [NOVA-FEATURE.md](NOVA-FEATURE.md) | for implementar qualquer coisa nova — é a receita passo a passo, da migration ao teste |
| [CONVENCOES.md](CONVENCOES.md) | precisar nomear uma classe, um DTO, um teste ou uma rota |
| [SEGURANCA.md](SEGURANCA.md) | for mexer em login, JWT, refresh token, CORS ou regra de permissão |
| [ERROS.md](ERROS.md) | precisar escolher um status HTTP ou criar uma exceção nova |
| [BANCO-DE-DADOS.md](BANCO-DE-DADOS.md) | for mexer em entidade, migration, auditoria ou ordenação por `position` |
| [TESTES.md](TESTES.md) | for escrever ou consertar um teste |

Para subir o projeto (local, Docker, dev e produção) e para backup, veja o
[README](../README.md). A referência de endpoints é o Scalar, em
`http://localhost:8080/scalar` — gerado a partir das anotações dos controllers,
então está sempre em dia com o código.

## Como manter isto vivo

Estes documentos descrevem o padrão que o código **de fato** segue. Se você
mudar o padrão, mude o documento no mesmo commit. Documentação que descreve um
projeto que não existe mais é pior do que documentação nenhuma.
