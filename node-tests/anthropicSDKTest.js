import Anthropic from '@anthropic-ai/sdk';
import readline from 'readline';

// Inicializa o cliente da Anthropic (vai procurar automaticamente a variável de ambiente ANTHROPIC_API_KEY)
const anthropic = new Anthropic({
    baseURL: 'http://localhost:1900', // Ajusta consoante a porta do teu servidor
    apiKey: 'dummy-key'               // Passamos um valor falso só para calar a validação do SDK
});

// Configura o readline para capturar o "Enter" na consola
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

// Função auxiliar que cria uma Promessa que só resolve quando o utilizador carrega no Enter
const aguardarEnter = () => {
    return new Promise((resolve) => {
        rl.question('\n Pressiona [ENTER] para o próximo pedido...\n', () => {
            resolve();
        });
    });
};

// A nossa lista de 10 pedidos (prompts)
const prompts = [
    "Explica-me o que é o Node.js numa frase curta.",
    "Qual é a diferença entre let e const em JavaScript?",
    "Diz-me uma curiosidade sobre o espaço sideral.",
    "Resume o conceito de Clean Architecture.",
    "Qual é a montanha mais alta do nosso sistema solar?",
    "Escreve um pequeno poema sobre programar à noite.",
    "O que é o Event Loop em JavaScript?",
    "Lista 3 vantagens de usar TypeScript.",
    "Como explicarias o que é uma API a uma criança de 5 anos?",
    "Diz-me uma piada curta sobre programadores."
];

async function executarPedidos() {
    console.log("A iniciar a execução sequencial de 10 pedidos à Anthropic...\n");

    for (let i = 0; i < prompts.length; i++) {
        const promptAtual = prompts[i];

        console.log(`--------------------------------------------------`);
        console.log(`Pedido ${i + 1}/10: "${promptAtual}"`);
        console.log(`A aguardar resposta da Anthropic...`);

        try {
            // Fazer o pedido à API do Claude (usando o modelo Haiku por ser mais rápido e barato para testes)
            const response = await anthropic.messages.create({
                model: "llama-3.3-70b-versatile",
                max_tokens: 500,
                messages: [
                    { role: "user", content: promptAtual }
                ]
            });

            // Extrair e imprimir a resposta
            const textoResposta = response.content[0].text;
            console.log(`\nResposta da Anthropic:\n${textoResposta}`);

        } catch (error) {
            console.error(`\nErro ao fazer o pedido:`, error.message);
        }

        // Se não for o último pedido, aguarda pelo Enter do utilizador
        if (i < prompts.length - 1) {
            await aguardarEnter();
        } else {
            console.log(`\n--------------------------------------------------`);
            console.log("Todos os 10 pedidos foram concluídos com sucesso!");
            rl.close(); // Fecha a interface do terminal para o script terminar
        }
    }
}

// Executar a função principal
executarPedidos();