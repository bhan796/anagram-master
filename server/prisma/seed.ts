import { prisma } from "../src/config/prisma.js";

const main = async (): Promise<void> => {
  await prisma.player.create({
    data: { name: "Seed Player" }
  });
};

main()
  .then(async () => {
    await prisma.$disconnect();
  })
  .catch(async (error) => {
    console.error(error);
    await prisma.$disconnect();
    process.exit(1);
  });