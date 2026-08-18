-- =====================================================================
--  SEED DE DESENVOLVIMENTO — ESTE SCRIPT APAGA TODOS OS DADOS DO BANCO
-- =====================================================================
--
--  Ele NÃO é uma migration de schema. Mora em db/seed/, pasta que só entra
--  no spring.flyway.locations quando o profile `dev` está ativo (veja
--  application-dev.properties). Produção não ativa profile nenhum e os
--  testes de integração também não, então nenhum dos dois enxerga este
--  arquivo.
--
--  Sendo uma repeatable migration (R__), o Flyway reaplica sozinho toda vez
--  que o conteúdo muda. Para rodar de novo sem editar nada:
--  .\scripts\seed-reset.ps1
--
--  Senha de todos os usuários: senha123
--
--  Duas regras que o script inteiro respeita:
--    * `position` começa em 0 e não tem buraco — é a invariante que o
--      BoardListService e o CardService mantêm em produção;
--    * nada de random(). A variação vem de aritmética sobre o índice da
--      série, então duas execuções geram os mesmos dados. A única exceção
--      é o salt do bcrypt, aleatório por definição: o hash muda a cada
--      rodada, a senha não.
-- =====================================================================

-- crypt()/gen_salt('bf') produzem $2a$10$…, o mesmo formato do
-- BCryptPasswordEncoder; digest(…,'sha256') em hex bate com Tokens.sha256.
--
-- O IF NOT EXISTS do CREATE EXTENSION emite um aviso quando a extensão já
-- está lá, e aviso no log de boot que não é problema só ensina a ignorar
-- aviso. Daí o teste explícito.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') THEN
        CREATE EXTENSION pgcrypto;
    END IF;
END $$;

TRUNCATE TABLE
    card_changes, attachments, card_links, comment_reactions, comments,
    checklist_items, card_assignees, card_labels, cards, labels,
    board_lists, board_members, boards, team_invites, team_members, teams,
    user_social_links, refresh_tokens, users
    RESTART IDENTITY CASCADE;


-- ---------------------------------------------------------------------
-- Usuários
-- ---------------------------------------------------------------------
-- Isabela entra de propósito com o perfil vazio (sem bio, telefone ou
-- endereço): é o estado de quem acabou de se cadastrar e nunca abriu o
-- perfil. Os quatro primeiros têm avatar — os arquivos vêm do
-- scripts/seed-uploads.ps1.
INSERT INTO users (name, email, password_hash, birth_date, phone, bio, avatar_path,
                   street, number, complement, city, state, zip_code,
                   created_at, updated_at)
VALUES
    ('Sérgio Fagundes', 'sergio@gmail.com', crypt('senha123', gen_salt('bf', 10)),
     '1993-04-17', '(48) 99812-4477',
     'Desenvolvedor back-end. Cuido da API do Planix e brigo com o Flyway nas horas vagas.',
     'profile_pictures/seed-avatar-1.png',
     'Rua Lauro Linhares', '1250', 'Sala 302', 'Florianópolis', 'SC', '88036-002',
     now() - interval '412 days', now() - interval '9 days'),

    ('Ana Souza', 'ana.souza@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1990-11-02', '(11) 98844-1290',
     'Diretora de criação no Estúdio Aurora. Gosto de briefing curto e de prazo realista.',
     'profile_pictures/seed-avatar-2.png',
     'Avenida Paulista', '900', 'Conjunto 71', 'São Paulo', 'SP', '01310-100',
     now() - interval '388 days', now() - interval '3 days'),

    ('Bruno Lima', 'bruno.lima@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1988-07-25', '(21) 99120-3355',
     'Tech lead. Se está quebrado em produção, provavelmente estou olhando o log.',
     'profile_pictures/seed-avatar-3.png',
     'Rua Voluntários da Pátria', '45', NULL, 'Rio de Janeiro', 'RJ', '22270-000',
     now() - interval '365 days', now() - interval '21 days'),

    ('Carla Mendes', 'carla.mendes@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1995-02-08', '(31) 98701-6644',
     'Product designer. Passo o dia entre entrevista de usuário e Figma.',
     'profile_pictures/seed-avatar-4.png',
     'Rua da Bahia', '1148', 'Apto 902', 'Belo Horizonte', 'MG', '30160-011',
     now() - interval '340 days', now() - interval '2 days'),

    ('Diego Rocha', 'diego.rocha@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1986-09-30', '(51) 99433-8812',
     'SRE. Meu trabalho é fazer o plantão ser chato.', NULL,
     'Avenida Ipiranga', '6681', 'Prédio 32', 'Porto Alegre', 'RS', '90619-900',
     now() - interval '311 days', now() - interval '15 days'),

    ('Elisa Prado', 'elisa.prado@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1997-06-14', '(85) 98122-9010',
     'Analista de produto. Vivo de métrica de ativação e de conversa com suporte.', NULL,
     'Rua Barão de Aracati', '300', NULL, 'Fortaleza', 'CE', '60115-080',
     now() - interval '287 days', now() - interval '30 days'),

    ('Felipe Antunes', 'felipe.antunes@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1992-12-03', '(41) 99655-2107', 'Front-end. Acredito em componente pequeno.', NULL,
     'Rua Comendador Araújo', '499', 'Sala 12', 'Curitiba', 'PR', '80420-000',
     now() - interval '260 days', now() - interval '6 days'),

    ('Gabriela Dias', 'gabriela.dias@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1994-03-21', '(71) 98330-7788',
     'QA. Se der para quebrar de um jeito estranho, eu acho.', NULL,
     'Avenida Sete de Setembro', '1010', NULL, 'Salvador', 'BA', '40060-001',
     now() - interval '245 days', now() - interval '11 days'),

    ('Henrique Matos', 'henrique.matos@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1989-08-11', '(62) 99201-4433', 'Infra e banco de dados. Backup testado é backup.', NULL,
     'Rua 84', '399', 'Quadra F-7', 'Goiânia', 'GO', '74093-060',
     now() - interval '198 days', now() - interval '4 days'),

    -- Perfil intencionalmente vazio.
    ('Isabela Nunes', 'isabela.nunes@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     now() - interval '46 days', now() - interval '46 days'),

    -- Só tem a própria equipe: é o caso de quem acabou de se cadastrar e
    -- ainda vai ser convidado para algum lugar.
    ('João Pereira', 'joao.pereira@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '2000-01-19', '(27) 99877-0021', 'Estagiário de desenvolvimento, começando agora.', NULL,
     'Rua Sete de Setembro', '88', NULL, 'Vitória', 'ES', '29015-000',
     now() - interval '18 days', now() - interval '18 days'),

    ('Lara Figueiredo', 'lara.figueiredo@planix.dev', crypt('senha123', gen_salt('bf', 10)),
     '1991-05-27', '(81) 98455-3390', 'Redatora. Escrevo o que o botão diz.', NULL,
     'Rua da Aurora', '325', 'Apto 604', 'Recife', 'PE', '50050-000',
     now() - interval '150 days', now() - interval '8 days');


-- ---------------------------------------------------------------------
-- Redes sociais (cobre os 7 valores de SocialPlatform)
-- ---------------------------------------------------------------------
INSERT INTO user_social_links (user_id, platform, url, created_at, updated_at)
SELECT u.id, v.platform, v.url, now() - interval '60 days', now() - interval '60 days'
  FROM (VALUES
        ('sergio@gmail.com',              'GITHUB',    'https://github.com/sergiofagundes'),
        ('sergio@gmail.com',              'LINKEDIN',  'https://linkedin.com/in/sergiofagundes'),
        ('sergio@gmail.com',              'WEBSITE',   'https://sergiofagundes.dev'),
        ('ana.souza@planix.dev',          'INSTAGRAM', 'https://instagram.com/anasouza.design'),
        ('ana.souza@planix.dev',          'LINKEDIN',  'https://linkedin.com/in/anasouza'),
        ('ana.souza@planix.dev',          'X',         'https://x.com/anasouza'),
        ('bruno.lima@planix.dev',         'GITHUB',    'https://github.com/brunolima'),
        ('bruno.lima@planix.dev',         'X',         'https://x.com/brunolima_dev'),
        ('carla.mendes@planix.dev',       'INSTAGRAM', 'https://instagram.com/carla.ux'),
        ('carla.mendes@planix.dev',       'WEBSITE',   'https://carlamendes.design'),
        ('carla.mendes@planix.dev',       'YOUTUBE',   'https://youtube.com/@carlamendesux'),
        ('diego.rocha@planix.dev',        'GITHUB',    'https://github.com/diegorocha'),
        ('diego.rocha@planix.dev',        'LINKEDIN',  'https://linkedin.com/in/diegorocha'),
        ('elisa.prado@planix.dev',        'LINKEDIN',  'https://linkedin.com/in/elisaprado'),
        ('felipe.antunes@planix.dev',     'GITHUB',    'https://github.com/felipeantunes'),
        ('felipe.antunes@planix.dev',     'FACEBOOK',  'https://facebook.com/felipe.antunes'),
        ('gabriela.dias@planix.dev',      'LINKEDIN',  'https://linkedin.com/in/gabrieladias'),
        ('henrique.matos@planix.dev',     'GITHUB',    'https://github.com/henriquematos'),
        ('lara.figueiredo@planix.dev',    'INSTAGRAM', 'https://instagram.com/lara.escreve'),
        ('lara.figueiredo@planix.dev',    'WEBSITE',   'https://larafigueiredo.com')
       ) AS v(email, platform, url)
  JOIN users u ON u.email = v.email;


-- ---------------------------------------------------------------------
-- Equipes e membros
-- ---------------------------------------------------------------------
INSERT INTO teams (owner_id, name, description, icon, created_at, updated_at)
SELECT u.id, v.name, v.description, v.icon,
       now() - (v.dias || ' days')::interval, now() - interval '5 days'
  FROM (VALUES
        ('sergio@gmail.com',       'Núcleo de Produto',
         'Time que toca o produto de ponta a ponta: descoberta, entrega e suporte.', 'rocket', 400),
        ('ana.souza@planix.dev',   'Estúdio Aurora',
         'Design, conteúdo e campanhas. Atende o produto e os clientes externos.', 'palette', 380),
        ('diego.rocha@planix.dev', 'Ops & Infra',
         'Infraestrutura, plantão e tudo que precisa acordar às três da manhã.', 'server', 300),
        ('joao.pereira@planix.dev','Equipe de João Pereira', NULL, NULL, 18)
       ) AS v(email, name, description, icon, dias)
  JOIN users u ON u.email = v.email;

INSERT INTO team_members (team_id, user_id, role, created_at)
SELECT t.id, u.id, v.role, t.created_at + interval '1 hour'
  FROM (VALUES
        ('Núcleo de Produto',       'sergio@gmail.com',           'OWNER'),
        ('Núcleo de Produto',       'bruno.lima@planix.dev',      'ADMIN'),
        ('Núcleo de Produto',       'carla.mendes@planix.dev',    'MEMBER'),
        ('Núcleo de Produto',       'elisa.prado@planix.dev',     'MEMBER'),
        ('Núcleo de Produto',       'felipe.antunes@planix.dev',  'MEMBER'),
        ('Núcleo de Produto',       'gabriela.dias@planix.dev',   'MEMBER'),
        ('Núcleo de Produto',       'henrique.matos@planix.dev',  'MEMBER'),

        ('Estúdio Aurora',          'ana.souza@planix.dev',       'OWNER'),
        ('Estúdio Aurora',          'sergio@gmail.com',           'ADMIN'),
        ('Estúdio Aurora',          'carla.mendes@planix.dev',    'ADMIN'),
        ('Estúdio Aurora',          'isabela.nunes@planix.dev',   'MEMBER'),
        ('Estúdio Aurora',          'lara.figueiredo@planix.dev', 'MEMBER'),

        ('Ops & Infra',             'diego.rocha@planix.dev',     'OWNER'),
        ('Ops & Infra',             'henrique.matos@planix.dev',  'ADMIN'),
        ('Ops & Infra',             'sergio@gmail.com',           'MEMBER'),
        ('Ops & Infra',             'bruno.lima@planix.dev',      'MEMBER'),

        ('Equipe de João Pereira',  'joao.pereira@planix.dev',    'OWNER')
       ) AS v(team, email, role)
  JOIN teams t ON t.name = v.team
  JOIN users u ON u.email = v.email;

-- Os quatro estados possíveis de um convite. O token em claro é o texto
-- antes do digest — está documentado em docs/BANCO-DE-DADOS.md.
INSERT INTO team_invites (team_id, created_by, token_hash, role, max_uses, uses,
                          expires_at, revoked_at, created_at)
SELECT t.id, u.id, encode(digest(v.token, 'sha256'), 'hex'), v.role,
       v.max_uses, v.uses,
       now() + (v.expira_em_dias || ' days')::interval,
       CASE WHEN v.revogado THEN now() - interval '2 days' END,
       now() - interval '10 days'
  FROM (VALUES
        ('Núcleo de Produto', 'sergio@gmail.com',     'convite-nucleo-de-produto',
         'MEMBER', 25,  3, 30,  false),
        ('Estúdio Aurora',    'ana.souza@planix.dev', 'convite-estudio-aurora',
         'ADMIN',   5,  5, 30,  false),   -- esgotado
        ('Ops & Infra',       'diego.rocha@planix.dev','convite-ops-expirado',
         'MEMBER', 10,  2, -3,  false),   -- expirado
        ('Ops & Infra',       'diego.rocha@planix.dev','convite-ops-revogado',
         'MEMBER', 10,  0, 20,  true)     -- revogado
       ) AS v(team, email, token, role, max_uses, uses, expira_em_dias, revogado)
  JOIN teams t ON t.name = v.team
  JOIN users u ON u.email = v.email;


-- ---------------------------------------------------------------------
-- Quadros
-- ---------------------------------------------------------------------
-- "Ideias Soltas" fica sem lista nenhuma de propósito: é o estado de quadro
-- recém-criado. Os RESTRICTED só aparecem para quem está em board_members.
INSERT INTO boards (team_id, owner_id, name, description, icon, visibility, created_at, updated_at)
SELECT t.id, u.id, v.name, v.description, v.icon, v.visibility,
       now() - (v.dias || ' days')::interval, now() - interval '1 day'
  FROM (VALUES
        ('Núcleo de Produto', 'sergio@gmail.com',          'Roadmap 2026',
         'O que a gente promete entregar neste ano, em ordem de prioridade.',
         'rocket', 'TEAM', 200),
        ('Núcleo de Produto', 'bruno.lima@planix.dev',     'Bugs e Incidentes',
         'Tudo que está quebrado, do relatado ao fechado.',
         'bug', 'TEAM', 190),
        ('Núcleo de Produto', 'carla.mendes@planix.dev',   'Descoberta de Produto',
         'Entrevistas, pesquisas e o que aprendemos com elas.',
         'compass', 'TEAM', 170),
        ('Núcleo de Produto', 'sergio@gmail.com',          'Financeiro Confidencial',
         'Contas, licenças e orçamento. Restrito a quem foi convidado.',
         'briefcase', 'RESTRICTED', 160),

        ('Estúdio Aurora',    'ana.souza@planix.dev',      'Campanha Primavera',
         'Peças, prazos e aprovações da campanha de lançamento.',
         'megaphone', 'TEAM', 150),
        ('Estúdio Aurora',    'carla.mendes@planix.dev',   'Design System',
         'Componentes, tokens e a documentação de cada um.',
         'palette', 'TEAM', 140),
        ('Estúdio Aurora',    'ana.souza@planix.dev',      'Contratos e Jurídico',
         'Minutas e assinaturas. Restrito.',
         'shield', 'RESTRICTED', 120),

        ('Ops & Infra',       'diego.rocha@planix.dev',    'Infraestrutura',
         'Provisionamento, monitoramento e automação do dia a dia.',
         'database', 'TEAM', 110),
        ('Ops & Infra',       'henrique.matos@planix.dev', 'Plantão e On-call',
         'Incidentes do plantão, do alerta ao post-mortem.',
         'gauge', 'TEAM', 90),
        ('Ops & Infra',       'diego.rocha@planix.dev',    'Migração de Cloud',
         'O projeto de mudança de provedor, fase a fase.',
         'globe', 'RESTRICTED', 70),

        ('Equipe de João Pereira', 'joao.pereira@planix.dev', 'Ideias Soltas',
         'Quadro novo, ainda sem nenhuma lista.',
         'lightbulb', 'TEAM', 15)
       ) AS v(team, email, name, description, icon, visibility, dias)
  JOIN teams t ON t.name = v.team
  JOIN users u ON u.email = v.email;

-- O dono sempre entra como membro — é o que o BoardService.create faz.
INSERT INTO board_members (board_id, user_id, created_at)
SELECT b.id, b.owner_id, b.created_at FROM boards b;

INSERT INTO board_members (board_id, user_id, created_at)
SELECT b.id, u.id, b.created_at + interval '2 hours'
  FROM (VALUES
        ('Roadmap 2026',            'bruno.lima@planix.dev'),
        ('Roadmap 2026',            'carla.mendes@planix.dev'),
        ('Roadmap 2026',            'elisa.prado@planix.dev'),
        ('Roadmap 2026',            'felipe.antunes@planix.dev'),
        ('Bugs e Incidentes',       'sergio@gmail.com'),
        ('Bugs e Incidentes',       'gabriela.dias@planix.dev'),
        ('Bugs e Incidentes',       'felipe.antunes@planix.dev'),
        ('Descoberta de Produto',   'elisa.prado@planix.dev'),
        ('Descoberta de Produto',   'sergio@gmail.com'),
        -- Restrito: só duas pessoas enxergam.
        ('Financeiro Confidencial', 'bruno.lima@planix.dev'),

        ('Campanha Primavera',      'lara.figueiredo@planix.dev'),
        ('Campanha Primavera',      'carla.mendes@planix.dev'),
        ('Campanha Primavera',      'isabela.nunes@planix.dev'),
        ('Design System',           'ana.souza@planix.dev'),
        ('Design System',           'felipe.antunes@planix.dev'),
        ('Contratos e Jurídico',    'sergio@gmail.com'),

        ('Infraestrutura',          'henrique.matos@planix.dev'),
        ('Infraestrutura',          'bruno.lima@planix.dev'),
        ('Infraestrutura',          'sergio@gmail.com'),
        ('Plantão e On-call',       'diego.rocha@planix.dev'),
        ('Plantão e On-call',       'bruno.lima@planix.dev'),
        ('Migração de Cloud',       'henrique.matos@planix.dev'),
        ('Migração de Cloud',       'sergio@gmail.com')
       ) AS v(board, email)
  JOIN boards b ON b.name = v.board
  JOIN users u ON u.email = v.email;


-- ---------------------------------------------------------------------
-- Listas
-- ---------------------------------------------------------------------
INSERT INTO board_lists (board_id, name, position, created_at, updated_at)
SELECT b.id, v.name, v.position, b.created_at + interval '1 hour', now() - interval '1 day'
  FROM (VALUES
        ('Roadmap 2026', 'Backlog', 0), ('Roadmap 2026', 'Priorizado', 1),
        ('Roadmap 2026', 'Em andamento', 2), ('Roadmap 2026', 'Em revisão', 3),
        ('Roadmap 2026', 'Concluído', 4),

        ('Bugs e Incidentes', 'Triagem', 0), ('Bugs e Incidentes', 'Confirmado', 1),
        ('Bugs e Incidentes', 'Corrigindo', 2), ('Bugs e Incidentes', 'Aguardando deploy', 3),
        ('Bugs e Incidentes', 'Fechado', 4),

        ('Descoberta de Produto', 'Ideias', 0), ('Descoberta de Produto', 'Entrevistas', 1),
        ('Descoberta de Produto', 'Sintetizando', 2), ('Descoberta de Produto', 'Validado', 3),

        ('Financeiro Confidencial', 'Orçamento', 0),
        ('Financeiro Confidencial', 'Aprovação', 1),
        ('Financeiro Confidencial', 'Pago', 2),

        ('Campanha Primavera', 'Briefing', 0), ('Campanha Primavera', 'Produção', 1),
        ('Campanha Primavera', 'Aprovação do cliente', 2), ('Campanha Primavera', 'Publicado', 3),

        ('Design System', 'Backlog', 0), ('Design System', 'Desenho', 1),
        ('Design System', 'Implementação', 2), ('Design System', 'Documentado', 3),

        ('Contratos e Jurídico', 'Minuta', 0),
        ('Contratos e Jurídico', 'Em revisão jurídica', 1),
        ('Contratos e Jurídico', 'Assinado', 2),

        ('Infraestrutura', 'Backlog', 0), ('Infraestrutura', 'Sprint atual', 1),
        ('Infraestrutura', 'Em andamento', 2), ('Infraestrutura', 'Bloqueado', 3),
        ('Infraestrutura', 'Concluído', 4),

        ('Plantão e On-call', 'Aberto', 0), ('Plantão e On-call', 'Investigando', 1),
        ('Plantão e On-call', 'Mitigado', 2), ('Plantão e On-call', 'Post-mortem', 3),

        ('Migração de Cloud', 'Levantamento', 0), ('Migração de Cloud', 'Piloto', 1),
        ('Migração de Cloud', 'Migrando', 2), ('Migração de Cloud', 'Validado', 3),
        ('Migração de Cloud', 'Concluído', 4)
       ) AS v(board, name, position)
  JOIN boards b ON b.name = v.board;


-- ---------------------------------------------------------------------
-- Etiquetas
-- ---------------------------------------------------------------------
-- As cores são os valores que o frontend conhece (src/lib/label-colors.ts).
-- Cada quadro leva as N primeiras do catálogo, com N variando de 5 a 8.
INSERT INTO labels (board_id, name, color, created_at, updated_at)
SELECT b.id, cat.name, cat.color, b.created_at + interval '3 hours', b.created_at + interval '3 hours'
  FROM boards b
  JOIN LATERAL (
        SELECT * FROM (VALUES
            (1, 'urgente',       'red'),
            (2, 'bug',           'orange'),
            (3, 'melhoria',      'green'),
            (4, 'documentação',  'blue'),
            (5, 'design',        'violet'),
            (6, 'backend',       'teal'),
            (7, 'frontend',      'pink'),
            (8, 'bloqueado',     'slate')
        ) AS c(ord, name, color)
       ) AS cat ON cat.ord <= 5 + (b.id % 4);


-- ---------------------------------------------------------------------
-- Cartões
-- ---------------------------------------------------------------------
-- O título vem de um vocabulário por quadro: 8 ações × 8 objetos = 64
-- combinações, escolhidas por um índice sequencial dentro do quadro (k).
-- Nenhum quadro chega perto de 64 cartões, então não há título repetido.
WITH vocab(board_name, acoes, objetos) AS (
    VALUES
    ('Roadmap 2026',
     ARRAY['Especificar','Priorizar','Prototipar','Validar','Lançar','Medir','Revisar','Documentar'],
     ARRAY['o onboarding novo','a busca global','o modo escuro','as notificações por e-mail',
           'o relatório semanal','a integração com o Slack','o plano gratuito','o app mobile']),
    ('Bugs e Incidentes',
     ARRAY['Investigar','Reproduzir','Corrigir','Testar','Monitorar','Reverter','Documentar','Fechar'],
     ARRAY['o erro 500 no login','a lentidão na listagem','o upload que falha acima de 5 MB',
           'o token que expira cedo demais','o filtro que ignora acentos','a exportação em branco',
           'o e-mail duplicado no cadastro','o scroll travado no Safari']),
    ('Descoberta de Produto',
     ARRAY['Entrevistar','Mapear','Analisar','Testar','Resumir','Apresentar','Revisar','Arquivar'],
     ARRAY['os usuários que abandonaram o cadastro','a jornada de convite',
           'os dados de churn do trimestre','a concorrência direta','as respostas da pesquisa NPS',
           'o funil de ativação','as sessões gravadas da semana','as ideias do canal de feedback']),
    ('Financeiro Confidencial',
     ARRAY['Levantar','Conferir','Aprovar','Pagar','Renegociar','Cancelar','Arquivar','Projetar'],
     ARRAY['a fatura da nuvem de janeiro','o contrato do Figma','as licenças da JetBrains',
           'o reembolso da conferência','o orçamento do trimestre','a nota fiscal do fornecedor',
           'o plano de saúde da equipe','os custos de domínio']),
    ('Campanha Primavera',
     ARRAY['Escrever','Desenhar','Aprovar','Agendar','Publicar','Impulsionar','Medir','Arquivar'],
     ARRAY['o post de lançamento','o e-mail para a base','o banner da home',
           'o vídeo de 30 segundos','a landing page','os stories da semana',
           'o anúncio no LinkedIn','o release para a imprensa']),
    ('Design System',
     ARRAY['Desenhar','Implementar','Documentar','Revisar','Migrar','Testar','Publicar','Depreciar'],
     ARRAY['o componente de botão','os tokens de cor','o campo de formulário',
           'o modal de confirmação','a escala tipográfica','o conjunto de ícones',
           'o tema escuro','a grade de layout']),
    ('Contratos e Jurídico',
     ARRAY['Redigir','Revisar','Ajustar','Enviar para assinatura','Assinar','Arquivar','Renovar','Encerrar'],
     ARRAY['o contrato do cliente Vertex','o acordo de confidencialidade da agência',
           'os termos de uso do site','a política de privacidade','o aditivo de prazo',
           'o contrato de estágio','o acordo de nível de serviço','a procuração']),
    ('Infraestrutura',
     ARRAY['Provisionar','Monitorar','Atualizar','Automatizar','Documentar','Revisar','Migrar','Testar'],
     ARRAY['o cluster de homologação','o backup do Postgres','os certificados TLS',
           'o pipeline de CI','os alertas do painel','o balanceador de carga',
           'as regras de firewall','o registro de imagens']),
    ('Plantão e On-call',
     ARRAY['Investigar','Mitigar','Escalar','Comunicar','Encerrar','Documentar','Revisar','Simular'],
     ARRAY['a queda da API às 3h','o disco cheio no nó 2','a fila de e-mails travada',
           'o pico de latência no banco','o certificado vencido','o job noturno que falhou',
           'o alerta falso do healthcheck','a indisponibilidade do CDN']),
    ('Migração de Cloud',
     ARRAY['Inventariar','Estimar','Migrar','Validar','Otimizar','Desligar','Documentar','Comunicar'],
     ARRAY['o banco de produção','os buckets de anexos','o DNS do domínio principal',
           'as filas de mensageria','o ambiente de homologação','os segredos e variáveis',
           'o monitoramento','os backups antigos'])
),
listas AS (
    SELECT bl.id AS list_id,
           b.name  AS board_name,
           bl.name AS list_name,
           bl.created_at,
           bl.position = max(bl.position) OVER (PARTITION BY bl.board_id) AS ultima,
           row_number() OVER (ORDER BY bl.board_id, bl.position)          AS rn
      FROM board_lists bl
      JOIN boards b ON b.id = bl.board_id
),
-- Dois casos de borda escolhidos pelo nome, não pelo índice: uma lista com
-- 22 cartões num quadro que todo mundo enxerga (scroll) e uma lista vazia.
quantidade AS (
    SELECT l.*,
           CASE WHEN (l.board_name, l.list_name) = ('Roadmap 2026', 'Backlog')        THEN 22
                WHEN (l.board_name, l.list_name) = ('Design System', 'Implementação') THEN 0
                ELSE 3 + (l.rn % 5) END AS n
      FROM listas l
),
gerados AS (
    SELECT q.list_id, q.board_name, q.created_at, q.ultima, q.rn, i,
           row_number() OVER (PARTITION BY q.board_name ORDER BY q.rn, i) - 1 AS k
      FROM quantidade q
      CROSS JOIN LATERAL generate_series(0, q.n - 1) AS i
)
INSERT INTO cards (list_id, title, description, due_date, priority, position,
                   completed, completed_at, created_at, updated_at)
SELECT g.list_id,
       v.acoes[1 + (g.k % 8)] || ' ' || v.objetos[1 + ((g.k / 8) % 8)],
       (ARRAY[
          NULL,
          E'## Contexto\n\nVeio da conversa com o time na última quinta.\n\n## Critério de pronto\n\n- passa nos testes\n- documentado',
          E'Herdado do quadro antigo. **Confirmar com quem pediu** antes de mexer.',
          NULL,
          E'### Passos\n\n1. levantar o que já existe\n2. propor a mudança\n3. validar com quem usa\n\n> Prazo é sugestão, não promessa.',
          E'Sem detalhe ainda — abrir a descrição na próxima refinada.'
       ]::text[])[1 + ((g.rn + g.i) % 6)],
       CASE (g.rn + g.i) % 5
            WHEN 1 THEN now() - ((1 + (g.k % 12)) || ' days')::interval   -- vencido
            WHEN 2 THEN now() + ((2 + (g.k % 21)) || ' days')::interval   -- a vencer
            WHEN 3 THEN date_trunc('day', now()) + interval '18 hours'    -- vence hoje
       END,
       (ARRAY['NONE','NONE','LOW','MEDIUM','HIGH','NONE','MEDIUM'])[1 + ((g.rn * 3 + g.i) % 7)],
       g.i,
       g.ultima OR (g.rn + g.i) % 11 = 0,
       CASE WHEN g.ultima OR (g.rn + g.i) % 11 = 0
            THEN now() - ((1 + (g.k % 20)) || ' days')::interval END,
       g.created_at + ((g.k % 60) || ' days')::interval,
       now() - ((g.k % 9) || ' days')::interval
  FROM gerados g
  JOIN vocab v ON v.board_name = g.board_name;


-- ---------------------------------------------------------------------
-- Etiquetas e responsáveis dos cartões
-- ---------------------------------------------------------------------
-- A etiqueta é sempre do quadro do cartão, e o responsável é sempre membro
-- do quadro — as duas coisas que a API garante e que um seed desatento quebra.
INSERT INTO card_labels (card_id, label_id)
SELECT c.id, l.id
  FROM cards c
  JOIN board_lists bl ON bl.id = c.list_id
  JOIN labels l ON l.board_id = bl.board_id
 WHERE (c.id * 3 + l.id) % 5 = 0;

INSERT INTO card_assignees (card_id, user_id)
SELECT c.id, bm.user_id
  FROM cards c
  JOIN board_lists bl ON bl.id = c.list_id
  JOIN board_members bm ON bm.board_id = bl.board_id
 WHERE (c.id + bm.user_id * 2) % 4 = 0;


-- ---------------------------------------------------------------------
-- Itens de checklist
-- ---------------------------------------------------------------------
WITH tarefas(ord, texto) AS (
    VALUES (0, 'Levantar os requisitos com quem pediu'),
           (1, 'Alinhar a abordagem com o time'),
           (2, 'Escrever os testes'),
           (3, 'Revisar com o design'),
           (4, 'Atualizar a documentação'),
           (5, 'Validar em homologação'),
           (6, 'Pedir aprovação do cliente'),
           (7, 'Medir o impacto depois de uma semana'),
           (8, 'Abrir o pull request'),
           (9, 'Fazer o deploy'),
           (10, 'Comunicar no canal do time'),
           (11, 'Registrar o aprendizado')
),
-- Metade dos cartões não tem checklist; os outros vão de 2 a 6 itens.
quantidade AS (
    SELECT c.id AS card_id, c.created_at,
           (ARRAY[0, 0, 0, 3, 5, 2, 0, 4, 0, 6])[1 + (c.id % 10)] AS n
      FROM cards c
)
INSERT INTO checklist_items (card_id, text, done, position, created_at, updated_at)
SELECT q.card_id, t.texto,
       -- Alguns cartões saem 100% concluídos, outros pela metade.
       CASE WHEN q.card_id % 7 = 0 THEN true ELSE i < (q.n + 1) / 2 END,
       i,
       q.created_at + interval '2 hours',
       q.created_at + ((1 + i) || ' hours')::interval
  FROM quantidade q
  CROSS JOIN LATERAL generate_series(0, q.n - 1) AS i
  JOIN tarefas t ON t.ord = (q.card_id * 3 + i) % 12;


-- ---------------------------------------------------------------------
-- Comentários: raízes, respostas e alguns apagados
-- ---------------------------------------------------------------------
-- O autor é sempre membro do quadro. `membros` guarda os ids de cada quadro
-- num array ordenado, e o índice sai de aritmética sobre o id do cartão —
-- assim o sorteio é determinístico e nunca aponta para fora do quadro.
WITH membros AS (
    SELECT board_id, array_agg(user_id ORDER BY user_id) AS ids
      FROM board_members GROUP BY board_id
),
falas(ord, texto) AS (
    VALUES (0,  'Alinhado com o time, seguimos por aqui.'),
           (1,  'Consegui reproduzir. Anexei o print no cartão.'),
           (2,  'Isso depende do cartão da lista ao lado, deixo bloqueado por ora.'),
           (3,  'Passei o olho e ficou ótimo. Só ajustaria o texto do botão.'),
           (4,  'Podemos fechar? Já está em produção desde ontem.'),
           (5,  'Movi para revisão, falta só o aval do design.'),
           (6,  'Estimativa: dois dias, contando o teste em homologação.'),
           (7,  'O cliente pediu para adiar essa parte para o mês que vem.'),
           (8,  'Atualizei a descrição com o que combinamos na reunião.'),
           (9,  'Quem consegue pegar isso hoje? Estou sem contexto suficiente.'),
           (10, 'Fiz o deploy e monitorei por uma hora, tudo estável.'),
           (11, 'Sugestão: quebrar em dois cartões, está grande demais.'),
           (12, 'Documentei tudo e deixei o link aqui embaixo.'),
           (13, 'Revertido. Voltamos para a versão anterior até entender a causa.'),
           (14, 'Boa! Isso resolve o problema que a Ana levantou na semana passada.')
),
quantidade AS (
    SELECT c.id AS card_id, bl.board_id, c.created_at,
           (ARRAY[0, 2, 1, 0, 3, 1, 2, 0])[1 + (c.id % 8)] AS n
      FROM cards c
      JOIN board_lists bl ON bl.id = c.list_id
)
INSERT INTO comments (card_id, user_id, parent_id, text, created_at, updated_at, deleted_at)
SELECT q.card_id,
       m.ids[1 + ((q.card_id * 5 + i) % array_length(m.ids, 1))],
       NULL,
       f.texto,
       q.created_at + ((6 + i * 5) || ' hours')::interval,
       q.created_at + ((6 + i * 5) || ' hours')::interval,
       -- Um punhado de comentários apagados, para ver o "comentário removido".
       CASE WHEN q.card_id % 37 = 0 AND i = 0 THEN now() - interval '3 days' END
  FROM quantidade q
  CROSS JOIN LATERAL generate_series(0, q.n - 1) AS i
  JOIN membros m ON m.board_id = q.board_id
  JOIN falas f ON f.ord = (q.card_id * 7 + i) % 15;

-- Respostas: parent_id aponta sempre para a raiz, nunca para outra resposta —
-- é a mesma normalização que o CommentService faz.
WITH membros AS (
    SELECT board_id, array_agg(user_id ORDER BY user_id) AS ids
      FROM board_members GROUP BY board_id
),
respostas(ord, texto) AS (
    VALUES (0, 'Concordo, pode seguir.'),
           (1, 'Fechado, cuido disso ainda hoje.'),
           (2, 'Prefiro esperar o retorno do cliente antes.'),
           (3, 'Já corrigi, dá uma olhada agora.'),
           (4, 'Faz sentido. Abri um cartão novo para a segunda parte.'),
           (5, 'Obrigado por checar!'),
           (6, 'Só um detalhe: falta atualizar a documentação também.'),
           (7, 'Não consegui reproduzir aqui. Qual navegador você usou?')
),
raizes AS (
    SELECT cm.id, cm.card_id, cm.created_at, bl.board_id
      FROM comments cm
      JOIN cards c ON c.id = cm.card_id
      JOIN board_lists bl ON bl.id = c.list_id
     WHERE cm.parent_id IS NULL AND cm.deleted_at IS NULL
       AND cm.id % 3 = 0
)
INSERT INTO comments (card_id, user_id, parent_id, text, created_at, updated_at, deleted_at)
SELECT r.card_id,
       m.ids[1 + ((r.id * 11 + i) % array_length(m.ids, 1))],
       r.id,
       resp.texto,
       r.created_at + ((2 + i * 3) || ' hours')::interval,
       r.created_at + ((2 + i * 3) || ' hours')::interval,
       NULL
  FROM raizes r
  CROSS JOIN LATERAL generate_series(0, CASE WHEN r.id % 9 = 0 THEN 1 ELSE 0 END) AS i
  JOIN membros m ON m.board_id = r.board_id
  JOIN respostas resp ON resp.ord = (r.id * 5 + i) % 8;


-- ---------------------------------------------------------------------
-- Reações
-- ---------------------------------------------------------------------
-- O emoji varia com i, então dois i diferentes nunca geram o mesmo par
-- (usuário, emoji) e o UNIQUE (comment_id, user_id, emoji) fica satisfeito.
WITH membros AS (
    SELECT board_id, array_agg(user_id ORDER BY user_id) AS ids
      FROM board_members GROUP BY board_id
),
alvo AS (
    SELECT cm.id, cm.created_at, bl.board_id,
           (ARRAY[0, 1, 0, 2, 3, 1, 0, 2])[1 + (cm.id % 8)] AS n
      FROM comments cm
      JOIN cards c ON c.id = cm.card_id
      JOIN board_lists bl ON bl.id = c.list_id
     WHERE cm.deleted_at IS NULL
)
INSERT INTO comment_reactions (comment_id, user_id, emoji, created_at, updated_at)
SELECT a.id,
       m.ids[1 + ((a.id * 13 + i * 3) % array_length(m.ids, 1))],
       (ARRAY['👍','❤️','🎉','🚀','👀','😄'])[1 + ((a.id + i) % 6)],
       a.created_at + interval '1 hour',
       a.created_at + interval '1 hour'
  FROM alvo a
  CROSS JOIN LATERAL generate_series(0, a.n - 1) AS i
  JOIN membros m ON m.board_id = a.board_id;


-- ---------------------------------------------------------------------
-- Links dos cartões
-- ---------------------------------------------------------------------
WITH catalogo(ord, url, titulo) AS (
    VALUES (0, 'https://docs.planix.dev/guia-de-contribuicao', 'Guia de contribuição'),
           (1, 'https://www.figma.com/file/planix-design-system', 'Figma do design system'),
           (2, 'https://github.com/sergio/planix/pull/128', 'Pull request #128'),
           (3, 'https://status.planix.dev/incidentes/2026-03-11', 'Relatório do incidente'),
           (4, 'https://www.notion.so/planix/ata-da-reuniao', 'Ata da reunião'),
           (5, 'https://planix.dev/blog/lancamento', NULL),
           (6, 'https://grafana.planix.dev/d/api-latency', 'Painel de latência'),
           (7, 'https://calendar.google.com/planix/retrospectiva', NULL)
)
INSERT INTO card_links (card_id, url, title, created_at, updated_at)
SELECT c.id, cat.url, cat.titulo,
       c.created_at + interval '5 hours', c.created_at + interval '5 hours'
  FROM cards c
  CROSS JOIN LATERAL generate_series(0, CASE WHEN c.id % 7 = 0 THEN 1
                                             WHEN c.id % 3 = 0 THEN 0
                                             ELSE -1 END) AS i
  JOIN catalogo cat ON cat.ord = (c.id * 3 + i) % 8;


-- ---------------------------------------------------------------------
-- Anexos
-- ---------------------------------------------------------------------
-- Os stored_filename apontam para arquivos que o scripts/seed-uploads.ps1
-- coloca no volume de uploads do dev. Sem rodar aquele script, o metadado
-- aparece na UI mas o download estoura StorageException.
-- Os tamanhos são os dos arquivos que o seed-uploads.ps1 gera; ele imprime a
-- lista no fim para conferência.
WITH arquivos(ord, original, guardado, tipo, tamanho) AS (
    VALUES (0, 'brief-produto.md',   'documents/seed-brief-produto.md',   'text/markdown',  497),
           (1, 'ata-reuniao.txt',    'documents/seed-ata-reuniao.txt',    'text/plain',     464),
           (2, 'especificacao.md',   'documents/seed-especificacao.md',   'text/markdown',  737),
           (3, 'mockup.png',         'documents/seed-mockup.png',         'image/png',     2000)
),
membros AS (
    SELECT board_id, array_agg(user_id ORDER BY user_id) AS ids
      FROM board_members GROUP BY board_id
),
escolhidos AS (
    SELECT * FROM (
        SELECT c.id AS card_id, c.created_at, bl.board_id,
               row_number() OVER (ORDER BY c.id) - 1 AS ord
          FROM cards c
          JOIN board_lists bl ON bl.id = c.list_id
         WHERE c.id % 17 = 0
    ) AS x
     ORDER BY x.ord
     LIMIT 12
)
INSERT INTO attachments (card_id, user_id, original_filename, stored_filename,
                         content_type, size_bytes, created_at)
SELECT e.card_id,
       m.ids[1 + (e.ord % array_length(m.ids, 1))],
       a.original, a.guardado, a.tipo, a.tamanho,
       e.created_at + interval '8 hours'
  FROM escolhidos e
  JOIN membros m ON m.board_id = e.board_id
  JOIN arquivos a ON a.ord = e.ord % 4;


-- ---------------------------------------------------------------------
-- Histórico de alterações
-- ---------------------------------------------------------------------
-- Só os `field` que o CardService realmente grava: title, description,
-- priority, due_date, completed e list_id.
WITH membros AS (
    SELECT board_id, array_agg(user_id ORDER BY user_id) AS ids
      FROM board_members GROUP BY board_id
),
base AS (
    SELECT c.id, c.title, c.priority, c.completed, c.created_at, bl.board_id
      FROM cards c
      JOIN board_lists bl ON bl.id = c.list_id
     WHERE c.id % 2 = 0 OR c.completed
)
INSERT INTO card_changes (card_id, changed_by, field, old_value, new_value, changed_at)
SELECT b.id,
       m.ids[1 + ((b.id * 7 + i) % array_length(m.ids, 1))],
       campo.field,
       CASE campo.field
            WHEN 'completed' THEN 'false'
            WHEN 'priority'  THEN 'NONE'
            WHEN 'title'     THEN 'Rascunho: ' || b.title
       END,
       CASE campo.field
            WHEN 'completed' THEN 'true'
            WHEN 'priority'  THEN b.priority
            WHEN 'title'     THEN b.title
            -- Mesmo formato que o CardService grava: OffsetDateTime.toString()
            -- em UTC, terminando em Z.
            WHEN 'due_date'  THEN to_char((b.created_at + interval '10 days') AT TIME ZONE 'UTC',
                                          'YYYY-MM-DD"T"HH24:MI:SS"Z"')
            ELSE 'Descrição preenchida na refinada.'
       END,
       b.created_at + ((12 + i * 9) || ' hours')::interval
  FROM base b
  JOIN membros m ON m.board_id = b.board_id
  CROSS JOIN LATERAL generate_series(0, CASE WHEN b.completed THEN 1 ELSE 0 END) AS i
  -- Cartão concluído ganha primeiro o registro do 'completed'; os demais
  -- campos entram por rodízio.
  CROSS JOIN LATERAL (
        SELECT (ARRAY['completed','priority','title','due_date','description'])[
                   CASE WHEN b.completed AND i = 0 THEN 1
                        ELSE 2 + ((b.id + i) % 4) END] AS field
       ) AS campo;


-- ---------------------------------------------------------------------
-- Resumo no log do boot
-- ---------------------------------------------------------------------
DO $$
DECLARE r record;
BEGIN
    SELECT (SELECT count(*) FROM users)             AS usuarios,
           (SELECT count(*) FROM user_social_links) AS redes,
           (SELECT count(*) FROM teams)             AS equipes,
           (SELECT count(*) FROM team_members)      AS membros_equipe,
           (SELECT count(*) FROM team_invites)      AS convites,
           (SELECT count(*) FROM boards)            AS quadros,
           (SELECT count(*) FROM board_members)     AS membros_quadro,
           (SELECT count(*) FROM board_lists)       AS listas,
           (SELECT count(*) FROM labels)            AS etiquetas,
           (SELECT count(*) FROM cards)             AS cartoes,
           (SELECT count(*) FROM card_labels)       AS cartao_etiqueta,
           (SELECT count(*) FROM card_assignees)    AS responsaveis,
           (SELECT count(*) FROM checklist_items)   AS checklists,
           (SELECT count(*) FROM comments)          AS comentarios,
           (SELECT count(*) FROM comment_reactions) AS reacoes,
           (SELECT count(*) FROM card_links)        AS links,
           (SELECT count(*) FROM attachments)       AS anexos,
           (SELECT count(*) FROM card_changes)      AS historico
      INTO r;

    RAISE NOTICE 'SEED DEV aplicado. Senha de todos os usuarios: senha123';
    RAISE NOTICE '  usuarios=% redes=% equipes=% membros_equipe=% convites=%',
                 r.usuarios, r.redes, r.equipes, r.membros_equipe, r.convites;
    RAISE NOTICE '  quadros=% membros_quadro=% listas=% etiquetas=%',
                 r.quadros, r.membros_quadro, r.listas, r.etiquetas;
    RAISE NOTICE '  cartoes=% etiquetas_em_cartao=% responsaveis=% checklists=%',
                 r.cartoes, r.cartao_etiqueta, r.responsaveis, r.checklists;
    RAISE NOTICE '  comentarios=% reacoes=% links=% anexos=% historico=%',
                 r.comentarios, r.reacoes, r.links, r.anexos, r.historico;
END $$;
